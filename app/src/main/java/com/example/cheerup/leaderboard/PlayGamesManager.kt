package com.example.cheerup.leaderboard

import android.app.Activity
import android.content.Context
import com.example.cheerup.R
import com.example.cheerup.domain.model.GameIdentifier
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import java.lang.ref.WeakReference

typealias AuthenticationProvider = () -> Boolean
typealias ScoreSubmitter = (score: Int, context: Int, leaderboardID: String, completion: (Throwable?) -> Unit) -> Unit

class PlayGamesManager {

    sealed class SubmissionError(override val message: String) : Exception(message) {
        data object InvalidLeaderboard : SubmissionError("Invalid leaderboard")
        data object NonPositiveScore : SubmissionError("Score must be positive")
        data object NotAuthenticated : SubmissionError("Player not authenticated")
    }

    private val stateLock = Any()
    private var _isAuthenticated: Boolean = false
    private var _lastAuthenticationError: Throwable? = null
    private var _lastSubmissionError: Throwable? = null
    private var _hasConfiguredAuthHandler: Boolean = false
    private var _isPlayerAuthenticatedProvider: AuthenticationProvider
    private var _scoreSubmitter: ScoreSubmitter

    private var appContext: Context? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    val isAuthenticated: Boolean
        get() = synchronized(stateLock) { _isAuthenticated }

    val lastAuthenticationError: Throwable?
        get() = synchronized(stateLock) { _lastAuthenticationError }

    val lastSubmissionError: Throwable?
        get() = synchronized(stateLock) { _lastSubmissionError }

    constructor() {
        this._isPlayerAuthenticatedProvider = { synchronized(stateLock) { _isAuthenticated } }
        this._scoreSubmitter = defaultScoreSubmitter
    }

    /** Designated constructor for isolated unit tests (matching iOS GameCenterManager). */
    constructor(
        isPlayerAuthenticated: AuthenticationProvider,
        scoreSubmitter: ScoreSubmitter
    ) {
        this._isPlayerAuthenticatedProvider = isPlayerAuthenticated
        this._scoreSubmitter = scoreSubmitter
        this._isAuthenticated = isPlayerAuthenticated()
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        try {
            PlayGamesSdk.initialize(context.applicationContext)
        } catch (_: Throwable) {
            // Safe no-op in unit test environment
        }
    }

    fun attachActivity(activity: Activity) {
        appContext = activity.applicationContext
        currentActivityRef = WeakReference(activity)
    }

    fun detachActivity(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
    }

    fun leaderboardID(forGame: GameIdentifier): String? = Companion.leaderboardID(forGame, appContext)

    // MARK: - Player Authentication (Play Games Services v2)

    fun authenticateLocalPlayer(activity: Activity? = currentActivityRef?.get()) {
        val shouldConfigure = synchronized(stateLock) {
            if (_hasConfiguredAuthHandler) false else {
                _hasConfiguredAuthHandler = true
                true
            }
        }
        if (!shouldConfigure && activity == null) return

        val act = activity ?: currentActivityRef?.get() ?: return
        try {
            val gamesSignInClient = PlayGames.getGamesSignInClient(act)
            gamesSignInClient.isAuthenticated.addOnCompleteListener { task ->
                val isAuth = task.isSuccessful && (task.result?.isAuthenticated == true)
                synchronized(stateLock) {
                    _isAuthenticated = isAuth
                    _lastAuthenticationError = task.exception
                }
            }
        } catch (t: Throwable) {
            synchronized(stateLock) {
                _isAuthenticated = false
                _lastAuthenticationError = t
            }
        }
    }

    fun signInInteractively(activity: Activity, onComplete: (Boolean) -> Unit = {}) {
        try {
            val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
            gamesSignInClient.signIn().addOnCompleteListener { task ->
                val isAuth = task.isSuccessful && (task.result?.isAuthenticated == true)
                synchronized(stateLock) {
                    _isAuthenticated = isAuth
                    _lastAuthenticationError = task.exception
                }
                onComplete(isAuth)
            }
        } catch (t: Throwable) {
            synchronized(stateLock) {
                _isAuthenticated = false
                _lastAuthenticationError = t
            }
            onComplete(false)
        }
    }

    fun showLeaderboard(activity: Activity, forGame: GameIdentifier, onUnavailable: () -> Unit = {}) {
        val id = leaderboardID(forGame)
        if (id == null) {
            onUnavailable()
            return
        }
        try {
            PlayGames.getLeaderboardsClient(activity)
                .getLeaderboardIntent(id)
                .addOnSuccessListener { intent ->
                    activity.startActivityForResult(intent, RC_LEADERBOARD_UI)
                }
                .addOnFailureListener {
                    onUnavailable()
                }
        } catch (_: Throwable) {
            onUnavailable()
        }
    }

    fun showAllLeaderboards(activity: Activity, onUnavailable: () -> Unit = {}) {
        try {
            PlayGames.getLeaderboardsClient(activity)
                .allLeaderboardsIntent
                .addOnSuccessListener { intent ->
                    activity.startActivityForResult(intent, RC_LEADERBOARD_UI)
                }
                .addOnFailureListener {
                    onUnavailable()
                }
        } catch (_: Throwable) {
            onUnavailable()
        }
    }

    // MARK: - Unified Score Submission

    fun submitScore(
        score: Int,
        forGame: GameIdentifier,
        completion: ((Result<Unit>) -> Unit)? = null
    ) {
        val id = leaderboardID(forGame)
        if (id == null) {
            completion?.invoke(Result.failure(SubmissionError.InvalidLeaderboard))
            return
        }
        if (score <= 0) {
            completion?.invoke(Result.failure(SubmissionError.NonPositiveScore))
            return
        }
        val isAuthProvider = synchronized(stateLock) { _isPlayerAuthenticatedProvider }
        if (!isAuthProvider()) {
            synchronized(stateLock) { _isAuthenticated = false }
            completion?.invoke(Result.failure(SubmissionError.NotAuthenticated))
            return
        }

        val submitter = synchronized(stateLock) { _scoreSubmitter }
        submitter(score, 0, id) { error ->
            synchronized(stateLock) {
                _lastSubmissionError = error
            }
            if (error != null) {
                completion?.invoke(Result.failure(error))
            } else {
                completion?.invoke(Result.success(Unit))
            }
        }
    }

    private val defaultScoreSubmitter: ScoreSubmitter = { score, _, leaderboardID, completion ->
        val act = currentActivityRef?.get()
        if (act != null) {
            try {
                PlayGames.getLeaderboardsClient(act)
                    .submitScoreImmediate(leaderboardID, score.toLong())
                    .addOnCompleteListener { task ->
                        completion(task.exception)
                    }
            } catch (t: Throwable) {
                completion(t)
            }
        } else {
            completion(null)
        }
    }

    // MARK: - Testing Support (Matching iOS GameCenterManager)

    fun configureForTesting(
        isPlayerAuthenticated: AuthenticationProvider,
        scoreSubmitter: ScoreSubmitter
    ) {
        synchronized(stateLock) {
            _isPlayerAuthenticatedProvider = isPlayerAuthenticated
            _scoreSubmitter = scoreSubmitter
            _isAuthenticated = isPlayerAuthenticated()
            _lastSubmissionError = null
        }
    }

    fun resetTestingConfiguration() {
        synchronized(stateLock) {
            _isPlayerAuthenticatedProvider = { synchronized(stateLock) { _isAuthenticated } }
            _scoreSubmitter = defaultScoreSubmitter
            _isAuthenticated = false
            _lastSubmissionError = null
        }
    }

    companion object {
        private const val RC_LEADERBOARD_UI = 9004
        val shared: PlayGamesManager by lazy { PlayGamesManager() }

        fun leaderboardID(forGame: GameIdentifier, context: Context? = null): String? {
            if (context != null) {
                try {
                    return when (forGame) {
                        GameIdentifier.TAP -> context.getString(R.string.leaderboard_tap_highscore)
                        GameIdentifier.PING_PONG -> context.getString(R.string.leaderboard_pingpong_highscore)
                        GameIdentifier.GRAVITY -> context.getString(R.string.leaderboard_gravity_highscore)
                        GameIdentifier.RUSH -> context.getString(R.string.leaderboard_rush_highscore)
                        GameIdentifier.CHESS -> context.getString(R.string.leaderboard_chess_wins)
                        GameIdentifier.WHEELS -> null
                    }
                } catch (_: Throwable) {
                    // Fallback to canonical IDs below
                }
            }
            return when (forGame) {
                GameIdentifier.TAP -> "com.gc.cheerup.tap.highscore"
                GameIdentifier.PING_PONG -> "com.gc.cheerup.pingpong.highscore"
                GameIdentifier.GRAVITY -> "com.gc.cheerup.gravity.highscore"
                GameIdentifier.RUSH -> "com.gc.cheerup.rush.highscore"
                GameIdentifier.CHESS -> "com.gc.cheerup.chess.wins"
                GameIdentifier.WHEELS -> null
            }
        }
    }
}
