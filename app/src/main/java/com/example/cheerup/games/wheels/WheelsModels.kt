package com.example.cheerup.games.wheels

import androidx.compose.ui.graphics.Color
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.domain.model.AppLanguage
import com.example.cheerup.domain.model.MascotMood
import java.util.UUID

enum class WheelOutcomeType(val rawValue: String) {
    HAPPY("happy"),
    CHAOS("chaos");

    val id: String get() = rawValue

    val badgeText: String
        get() = when (this) {
            HAPPY -> "💛 HAPPY"
            CHAOS -> "🌀 CHAOS"
        }

    val accentColor: Color
        get() = when (this) {
            HAPPY -> Color(0xFF22C55E)
            CHAOS -> Color(0xFFDF2955)
        }
}

data class WheelOutcome(
    val id: UUID = UUID.randomUUID(),
    val type: WheelOutcomeType,
    val emoji: String,
    val titleEn: String,
    val titleFr: String,
    val titleEs: String,
    val textEn: String,
    val textFr: String,
    val textEs: String,
    val mood: MascotMood,
    val timerSeconds: Int? = null,
    val weight: Int = 10
) {
    val title: String
        get() = when (LanguageManager.shared.currentLanguage) {
            AppLanguage.FRENCH -> titleFr
            AppLanguage.SPANISH -> titleEs
            AppLanguage.ENGLISH -> titleEn
        }

    val text: String
        get() = when (LanguageManager.shared.currentLanguage) {
            AppLanguage.FRENCH -> textFr
            AppLanguage.SPANISH -> textEs
            AppLanguage.ENGLISH -> textEn
        }
}

data class WheelSegment(
    val id: Int,
    val color: Color
)

data class WheelsConfettiParticle(
    val id: UUID = UUID.randomUUID(),
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Double,
    var vr: Double,
    var size: Float,
    var color: Color,
    var opacity: Double
)

object WheelsData {
    val segments: List<WheelSegment> = listOf(
        WheelSegment(id = 0,  color = Color(0xFFFF4B5C)), // Coral Red
        WheelSegment(id = 1,  color = Color(0xFFFF8838)), // Tangerine
        WheelSegment(id = 2,  color = Color(0xFF3D7BFF)), // Sky Blue
        WheelSegment(id = 3,  color = Color(0xFFFFC83D)), // Warm Yellow
        WheelSegment(id = 4,  color = Color(0xFF2EDB8A)), // Mint Green
        WheelSegment(id = 5,  color = Color(0xFF8B5CF6)), // Soft Violet
        WheelSegment(id = 6,  color = Color(0xFF028090)), // Deep Teal
        WheelSegment(id = 7,  color = Color(0xFFFF6B81)), // Rose Pink
        WheelSegment(id = 8,  color = Color(0xFF54A0FF)), // Soft Azure
        WheelSegment(id = 9,  color = Color(0xFFFECA57)), // Sunny Amber
        WheelSegment(id = 10, color = Color(0xFF1DD1A1)), // Fresh Emerald
        WheelSegment(id = 11, color = Color(0xFF5F27CD))  // Royal Indigo
    )

    val outcomes: List<WheelOutcome> = listOf(
        // Bizarre & Fun Challenges (26 Happy)
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🦩", titleEn = "Flamingo Stance", titleFr = "Flamant Rose", titleEs = "Flamenco", textEn = "Balance on one leg like a flamingo for 16 seconds.", textFr = "Tenez sur une seule jambe pendant 16s.", textEs = "Mantén el equilibrio en un pie 16s.", mood = MascotMood.SUS, timerSeconds = 16),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "👽", titleEn = "Alien Contact", titleFr = "Signal Alien", titleEs = "Contacto Alien", textEn = "Make high pitched alien beeping noises right now.", textFr = "Faites des bruits d'extraterrestre suraigus.", textEs = "Haz sonidos agudos de alien ya.", mood = MascotMood.WOW),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🐱", titleEn = "Meow Order", titleFr = "Ordre Miaou", titleEs = "Orden Miau", textEn = "Meow three times with total seriousness.", textFr = "Miaulez trois fois très sérieusement.", textEs = "Maúlla 3 veces con total seriedad.", mood = MascotMood.DIZZY),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🕶️", titleEn = "Slow Motion", titleFr = "Mode Ralenti", titleEs = "Cámara Lenta", textEn = "Do everything in dramatic slow motion for 20 seconds.", textFr = "Bougez au ralenti pendant 20 secondes.", textEs = "Muévete en cámara lenta durante 20s.", mood = MascotMood.SUS, timerSeconds = 20),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🦖", titleEn = "T Rex Mode", titleFr = "Mode T Rex", titleEs = "Modo T Rex", textEn = "Tuck your elbows in and let out a tiny T Rex roar.", textFr = "Rentrez les coudes et rugissez en T Rex.", textEs = "Pega los codos y ruge como un T Rex.", mood = MascotMood.PARTY),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🧼", titleEn = "Invisible Wash", titleFr = "Savon Imaginaire", titleEs = "Jabón Invisible", textEn = "Aggressively wash hands with imaginary soap.", textFr = "Frottez vos mains au savon invisible.", textEs = "Lávate las manos con jabón invisible.", mood = MascotMood.DIZZY, timerSeconds = 16),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🦀", titleEn = "Crab Walk", titleFr = "Mode Crabe", titleEs = "Cangrejo", textEn = "Take two sideways steps and snap your crab claws.", textFr = "Faites deux pas de côté en pinçant l'air.", textEs = "Da dos pasos de lado haciendo pinzas.", mood = MascotMood.SUS),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🧙‍♂️", titleEn = "Cast a Spell", titleFr = "Faux Sortilège", titleEs = "Hechizo", textEn = "Point at the nearest wall and shout a made up spell.", textFr = "Pointez le mur et criez un sort inventé.", textEs = "Apunta a la pared y grita un hechizo inventado.", mood = MascotMood.WOW),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🧘", titleEn = "Bizarre Zen", titleFr = "Zen Étrange", titleEs = "Zen Bizarro", textEn = "Chant a deep “Ooooom” until the timer runs out.", textFr = "Chantez un long « Oooom » jusqu'au bip.", textEs = "Entona un « Oooom » profundo hasta el timbre.", mood = MascotMood.HAPPY, timerSeconds = 16),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🤫", titleEn = "Breaking News", titleFr = "Flash Info", titleEs = "Última Hora", textEn = "Whisper urgent breaking news to your nearest object.", textFr = "Chuchotez un scoop urgent à un objet proche.", textEs = "Susurra una noticia urgente al objeto más cercano.", mood = MascotMood.DIZZY),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "😱", titleEn = "Soap Opera", titleFr = "Télénovela", titleEs = "Telenovela", textEn = "Gasp loudly like you just saw a telenovela plot twist.", textFr = "Prenez un air ultra choqué façon télénovela.", textEs = "Respira hondo con dramatismo de telenovela.", mood = MascotMood.WOW),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🐧", titleEn = "Penguin Waddle", titleFr = "Pingouin", titleEs = "Paso Pingüino", textEn = "Lock knees, flap arms, and waddle like a penguin.", textFr = "Bras collés, marchez comme un pingouin.", textEs = "Junta las rodillas y camina como pingüino.", mood = MascotMood.PARTY, timerSeconds = 14),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "👑", titleEn = "Royal Wave", titleFr = "Salut Royal", titleEs = "Saludo Real", textEn = "Stand up and give everyone a slow, dignified royal wave.", textFr = "Levez-vous et saluez tout le monde comme un roi.", textEs = "Ponte de pie y saluda a todos como la realeza.", mood = MascotMood.HAPPY, timerSeconds = 12),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "📸", titleEn = "Paparazzi Pose", titleFr = "Pose Paparazzi", titleEs = "Pose Paparazzi", textEn = "Strike 3 supermodel poses in a row like cameras are flashing.", textFr = "Enchaînez 3 poses de mannequin sous les flashs.", textEs = "Haz 3 poses seguidas de supermodelo para las cámaras.", mood = MascotMood.PARTY, timerSeconds = 16),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🎻", titleEn = "Tiny Violin", titleFr = "Mini Violon", titleEs = "Violín Miniatura", textEn = "Play the world's smallest sad violin for the group.", textFr = "Jouez du plus petit violon triste du monde.", textEs = "Toca el violín más pequeño y triste del mundo.", mood = MascotMood.SUS, timerSeconds = 14),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🏋️", titleEn = "Invisible Barbell", titleFr = "Haltère Invisible", titleEs = "Pesa Invisible", textEn = "Pretend to lift a 500-pound invisible barbell over your head.", textFr = "Soulevez une barre invisible de 200 kilos.", textEs = "Finge levantar una pesa invisible gigante.", mood = MascotMood.WOW, timerSeconds = 16),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🎤", titleEn = "Mic Drop", titleFr = "Lâcher de Micro", titleEs = "Suelta el Micro", textEn = "Give a 1-sentence motivational speech and drop an imaginary mic.", textFr = "Faites un discours d'une phrase et lâchez un micro imaginaire.", textEs = "Di una frase motivadora y suelta un micrófono imaginario.", mood = MascotMood.PARTY),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🕵️", titleEn = "Compliment Sniper", titleFr = "Compliment Express", titleEs = "Cumplido Veloz", textEn = "Give the person to your right a super specific compliment.", textFr = "Faites un compliment ultra précis à votre voisin de droite.", textEs = "Hazle un cumplido muy específico a quien esté a tu derecha.", mood = MascotMood.HAPPY),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🤠", titleEn = "Cowboy Draw", titleFr = "Duel Cowboy", titleEs = "Duelo Vaquero", textEn = "Squint your eyes and say “This town ain’t big enough for us.”", textFr = "Plissez les yeux : « Cette ville est trop petite pour nous. »", textEs = "Entrecierra los ojos y di: «Este pueblo es muy chico para los dos».", mood = MascotMood.SUS),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🏀", titleEn = "Buzzer Beater", titleFr = "Panier au Buzzer", titleEs = "Canasta Final", textEn = "Shoot an imaginary 3-pointer in slow motion and celebrate.", textFr = "Tirez un 3 points imaginaire au ralenti et célébrez !", textEs = "Lanza un triple imaginario en cámara lenta y celebra.", mood = MascotMood.PARTY),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🧞", titleEn = "Three Wishes", titleFr = "Trois Vœux", titleEs = "Tres Deseos", textEn = "Name 3 things you'd wish for from a genie in 16 seconds.", textFr = "Citez 3 vœux à demander à un génie en 16 secondes.", textEs = "Di 3 deseos que le pedirías a un genio en 16 segundos.", mood = MascotMood.WOW, timerSeconds = 16),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🥁", titleEn = "Table Drumroll", titleFr = "Roulement de Tambour", titleEs = "Redoble de Tambor", textEn = "Tap an epic drumroll on your knees and end with “TA-DA!”", textFr = "Faites un roulement de tambour sur vos genoux et criez « TADA ! »", textEs = "Haz un redoble en tus rodillas y termina diciendo «¡TA-CHÁN!»", mood = MascotMood.PARTY, timerSeconds = 12),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🦸", titleEn = "Superhero Landing", titleFr = "Pose Super-Héros", titleEs = "Aterrizaje Héroe", textEn = "Do a dramatic superhero landing pose and hold it.", textFr = "Prenez une pose d'atterrissage de super-héros.", textEs = "Haz una pose épica de aterrizaje de superhéroe.", mood = MascotMood.WOW, timerSeconds = 12),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🎬", titleEn = "Movie Trailer", titleFr = "Voix Bande-Annonce", titleEs = "Voz de Tráiler", textEn = "Describe what you had for breakfast in an epic movie trailer voice.", textFr = "Racontez votre petit-déjeuner avec une voix de bande-annonce.", textEs = "Cuenta qué desayunaste con voz épica de tráiler de cine.", mood = MascotMood.HAPPY),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🫡", titleEn = "Captain's Orders", titleFr = "Ordres du Capitaine", titleEs = "Orden del Capitán", textEn = "Salute the player across from you and say “Aye aye, Captain!”", textFr = "Saluez le joueur en face et dites « Oui, Capitaine ! »", textEs = "Saluda al jugador de enfrente y di «¡A la orden, Capitán!»", mood = MascotMood.HAPPY),
        WheelOutcome(type = WheelOutcomeType.HAPPY, emoji = "🦋", titleEn = "Blink Contest", titleFr = "Papillon Express", titleEs = "Pestañeo Rápido", textEn = "Flutter your eyelashes as fast as humanly possible for 10 seconds.", textFr = "Battez des cils le plus vite possible pendant 10s.", textEs = "Pestañea lo más rápido posible durante 10 segundos.", mood = MascotMood.DIZZY, timerSeconds = 10),

        // Chaos Outcomes (32 Chaos)
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🍕", titleEn = "Pizza Phone", titleFr = "Tel Pizza", titleEs = "Pizza Móvil", textEn = "Hold phone like pizza. Say “mmm”.", textFr = "Tel façon pizza. Dites « mmm ».", textEs = "Sostén el móvil como pizza. Di « mmm ».", mood = MascotMood.SUS),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🐔", titleEn = "Chicken Mode", titleFr = "Mode Poulet", titleEs = "Modo Gallina", textEn = "Cluck like a chicken for 20 seconds.", textFr = "Faites la poule pendant 20s.", textEs = "Cacarea como gallina durante 20s.", mood = MascotMood.WOW, timerSeconds = 20),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🙃", titleEn = "Upside Down", titleFr = "À l’envers", titleEs = "Del Revés", textEn = "Universe flipped. Enjoy.", textFr = "Monde inversé. Profitez.", textEs = "Universo invertido. Disfruta.", mood = MascotMood.DIZZY),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🕺", titleEn = "Dance Order", titleFr = "Danse !", titleEs = "¡A Bailar!", textEn = "Dance like nobody is watching.", textFr = "Dansez sans retenue.", textEs = "Baila como si nadie te mirara.", mood = MascotMood.PARTY, timerSeconds = 20),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🥔", titleEn = "Potato Alert", titleFr = "Alerte Patate", titleEs = "Alerta Patata", textEn = "You are now a potato.", textFr = "Vous êtes une patate.", textEs = "Ahora eres una patata.", mood = MascotMood.SUS),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🧦", titleEn = "Sock Mystery", titleFr = "Chaussettes", titleEs = "Calcetines", textEn = "Introduce two socks to each other.", textFr = "Présentez deux chaussettes.", textEs = "Presenta a dos calcetines entre sí.", mood = MascotMood.SUS),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "📢", titleEn = "Dramatic Entrance", titleFr = "Entrée Star", titleEs = "Entrada Triunfal", textEn = "Announce “I have arrived!”", textFr = "Dites « Je suis là ! »", textEs = "Anuncia con voz alta: «¡He llegado!»", mood = MascotMood.WOW),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🦆", titleEn = "Duck Invasion", titleFr = "Invasion Canard", titleEs = "Invasión Patos", textEn = "Ducks took over. Resistance is futile.", textFr = "Les canards attaquent !", textEs = "¡Los patos dominan! No te resistas.", mood = MascotMood.DIZZY),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🤫", titleEn = "Secret Mission", titleFr = "Mission Secrète", titleEs = "Misión Secreta", textEn = "Walk like a spy. Do not blink.", textFr = "Marchez comme un espion.", textEs = "Camina como espía. No parpadees.", mood = MascotMood.SUS, timerSeconds = 30),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🫠", titleEn = "CheerUp Meltdown", titleFr = "CheerUp Fond", titleEs = "CheerUp se Derrite", textEn = "CheerUp is melting into pixels.", textFr = "CheerUp fond en pixels.", textEs = "CheerUp se derrite en píxeles.", mood = MascotMood.DIZZY),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🗣️", titleEn = "Backwards Talk", titleFr = "À l'envers", titleEs = "Al Revés", textEn = "Say your name backwards out loud.", textFr = "Dites votre nom à l'envers.", textEs = "Di tu nombre al revés en voz alta.", mood = MascotMood.WOW),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🌀", titleEn = "Absolute Nonsense", titleFr = "Absurde", titleEs = "Disparate", textEn = "Banana telescope purple Tuesday.", textFr = "Banane télescope mardi violet.", textEs = "Plátano telescopio martes morado.", mood = MascotMood.DIZZY),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🚀", titleEn = "Rocket Launch", titleFr = "Fusée", titleEs = "Cohete", textEn = "Count 5 to 1 with rocket noises.", textFr = "Comptez 5 à 1 façon fusée.", textEs = "Cuenta 5 a 1 con ruidos de cohete.", mood = MascotMood.WOW, timerSeconds = 12),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "👁️", titleEn = "Stare Down", titleFr = "Fixette", titleEs = "Duelo Visual", textEn = "Stare without blinking for 16 seconds.", textFr = "Fixez 16s sans ciller.", textEs = "Mira sin parpadear durante 16s.", mood = MascotMood.SUS, timerSeconds = 16),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🧊", titleEn = "Freeze Frame", titleFr = "Statue", titleEs = "Estatua", textEn = "Freeze in your current pose. Don’t move!", textFr = "Ne bougez plus d'un poil !", textEs = "¡Quédate inmóvil en tu postura actual!", mood = MascotMood.WOW, timerSeconds = 14),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🤖", titleEn = "Robot Mode", titleFr = "Robot", titleEs = "Modo Robot", textEn = "Say “BEEP BOOP I AM HUMAN” out loud.", textFr = "Dites « BIP BOP HUMAIN ».", textEs = "Di « BIP BOP SOY HUMANO » en voz alta.", mood = MascotMood.DIZZY),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🎸", titleEn = "Air Guitar Solo", titleFr = "Air Guitar", titleEs = "Air Guitar", textEn = "Unleash an epic air guitar solo right now.", textFr = "Solo d'air guitar maintenant !", textEs = "¡Toca un solo épico de guitarra aérea!", mood = MascotMood.PARTY, timerSeconds = 16),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🗿", titleEn = "Stone Face", titleFr = "Visage de Pierre", titleEs = "Cara de Piedra", textEn = "No smiling allowed for 20 seconds.", textFr = "Interdit de sourire 20s.", textEs = "Prohibido sonreír durante 20s.", mood = MascotMood.SUS, timerSeconds = 20),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🎭", titleEn = "Accent Switch", titleFr = "Faux Accent", titleEs = "Cambio de Acento", textEn = "Speak in a dramatic pirate or fancy accent for your next 2 sentences.", textFr = "Parlez avec un accent de pirate pour vos 2 prochaines phrases.", textEs = "Habla con acento pirata o elegante en tus próximas 2 frases.", mood = MascotMood.PARTY),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🧟", titleEn = "Zombie Walk", titleFr = "Marche Zombie", titleEs = "Paso Zombie", textEn = "Arms out! Groan and walk like a hungry zombie for 16 seconds.", textFr = "Bras tendus ! Marchez comme un zombie pendant 16s.", textEs = "¡Brazos al frente! Camina como zombie durante 16 segundos.", mood = MascotMood.DIZZY, timerSeconds = 16),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🐸", titleEn = "Frog Leap", titleFr = "Saut de Grenouille", titleEs = "Salto de Rana", textEn = "Do one mini frog hop and say “RIBBIT!” with a straight face.", textFr = "Faites un petit saut de grenouille et dites « CROA ! »", textEs = "Da un pequeño salto de rana y di «¡CROAC!» muy serio.", mood = MascotMood.WOW),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "📣", titleEn = "Sports Announcer", titleFr = "Commentateur Sportif", titleEs = "Narrador Deportivo", textEn = "Narrate what the person next to you is doing like it's the World Cup final.", textFr = "Commentez ce que fait votre voisin comme en finale de Coupe du Monde.", textEs = "Narra lo que hace la persona a tu lado como final del Mundial.", mood = MascotMood.PARTY, timerSeconds = 20),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🤐", titleEn = "No Teeth Laugh", titleFr = "Rire Sans Dents", titleEs = "Risa Sin Dientes", textEn = "Laugh out loud without showing any teeth for 12 seconds.", textFr = "Riez à voix haute sans montrer vos dents pendant 12s.", textEs = "Ríete en voz alta sin mostrar los dientes durante 12s.", mood = MascotMood.DIZZY, timerSeconds = 12),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🐒", titleEn = "Chimp Cheer", titleFr = "Cri du Singe", titleEs = "Saludo Mono", textEn = "Scratch your head like a monkey and make 3 quick “ooh-ooh!” sounds.", textFr = "Grattez-vous la tête comme un singe en faisant « ouh-ouh ! »", textEs = "Ráscate la cabeza como mono y haz 3 sonidos de «¡uh-uh!»", mood = MascotMood.PARTY),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🥊", titleEn = "Shadow Boxer", titleFr = "Boxe Fantôme", titleEs = "Boxeo Sombra", textEn = "Throw 5 lightning-fast shadow boxing punches in the air with sound effects.", textFr = "Lancez 5 coups de boxe dans le vide avec les bruitages !", textEs = "Lanza 5 puñetazos al aire con efectos de sonido.", mood = MascotMood.WOW, timerSeconds = 12),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🫡", titleEn = "Thumb War Dare", titleFr = "Bataille de Pouces", titleEs = "Guerra de Pulgares", textEn = "Challenge the nearest player to a 20-second thumb war right now!", textFr = "Défiez le joueur le plus proche à une bataille de pouces de 20s !", textEs = "¡Reta al jugador más cercano a una guerra de pulgares de 20s!", mood = MascotMood.PARTY, timerSeconds = 20),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "✌️", titleEn = "Rock Paper Scissors", titleFr = "Pierre Feuille Ciseaux", titleEs = "Piedra Papel Tijera", textEn = "Play 1 sudden-death round of Rock Paper Scissors with the person to your left!", textFr = "Jouez une manche sèche de Pierre-Feuille-Ciseaux à votre gauche !", textEs = "¡Juega 1 ronda rápida de Piedra, Papel o Tijera a tu izquierda!", mood = MascotMood.WOW),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🎶", titleEn = "Hum That Tune", titleFr = "Fredonne un Tube", titleEs = "Tararea la Canción", textEn = "Hum a famous song for 16 seconds and see if anyone guesses it!", textFr = "Fredonnez une chanson connue pendant 16s pour la faire deviner !", textEs = "¡Tararea una canción famosa por 16s a ver quién la adivina!", mood = MascotMood.PARTY, timerSeconds = 16),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🪞", titleEn = "Mirror Mime", titleFr = "Mime Miroir", titleEs = "Mimo Espejo", textEn = "Copy every hand movement of the person across from you for 20 seconds.", textFr = "Imitez tous les gestes de la personne en face pendant 20s.", textEs = "Copia cada gesto de la persona frente a ti durante 20s.", mood = MascotMood.SUS, timerSeconds = 20),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🦁", titleEn = "Whisper Roar", titleFr = "Rugissement Chuchoté", titleEs = "Rugido en Susurro", textEn = "Do the fiercest lion face possible while whispering a tiny “rawr”.", textFr = "Faites une tête de lion féroce en chuchotant un mini « grrr ».", textEs = "Pon cara de león féroz pero susurra un «grrr» diminuto.", mood = MascotMood.SUS),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🕺", titleEn = "Moonwalk Attempt", titleFr = "Tentative Moonwalk", titleEs = "Intento de Moonwalk", textEn = "Attempt your best 10-second moonwalk right where you are.", textFr = "Tentez votre meilleur moonwalk pendant 10 secondes.", textEs = "Intenta tu mejor moonwalk durante 10 segundos ahora mismo.", mood = MascotMood.PARTY, timerSeconds = 10),
        WheelOutcome(type = WheelOutcomeType.CHAOS, emoji = "🦹", titleEn = "Evil Laugh", titleFr = "Rire Machiavélique", titleEs = "Risa de Villano", textEn = "Rub your hands together and let out a cartoon villain “MWAHAHA!”", textFr = "Frottez-vous les mains avec un rire de méchant « MOUAHAHA ! »", textEs = "Frótate las manos y suelta una risa de villano «¡MUAJAJA!»", mood = MascotMood.WOW)
    )

    val idleLines: List<String>
        get() = when (LanguageManager.shared.currentLanguage) {
            AppLanguage.FRENCH -> listOf("Fais tourner !", "Jour de chance ?", "Allez, tourne !", "Prêt quand tu veux !")
            AppLanguage.SPANISH -> listOf("¡Gírame!", "¿Día de suerte?", "¡Dale, gira!", "¡Listo cuando quieras!")
            AppLanguage.ENGLISH -> listOf("Spin me!", "Feeling lucky?", "Ooh, spin it!", "Ready when you are!")
        }

    val spinLines: List<String>
        get() = when (LanguageManager.shared.currentLanguage) {
            AppLanguage.FRENCH -> listOf("Youhouuu !", "Ça tourne !", "Où ça s'arrête ?", "Attention !")
            AppLanguage.SPANISH -> listOf("¡Yujuuu!", "¡Está girando!", "¿Dónde caerá?", "¡Agárrate a tus píxeles!")
            AppLanguage.ENGLISH -> listOf("Wheeeee!", "Whoa whoa whoa!", "Where will it land?", "Hold onto your pixels!")
        }

    val happyLines: List<String>
        get() = when (LanguageManager.shared.currentLanguage) {
            AppLanguage.FRENCH -> listOf("Défi relevé ?", "À vous de jouer !", "C'est parti !", "Montrez ce que vous valez !")
            AppLanguage.SPANISH -> listOf("¿Aceptas el reto?", "¡Tu turno!", "¡A ver cómo lo haces!", "¡A por todas!")
            AppLanguage.ENGLISH -> listOf("Challenge accepted?", "Your turn!", "Let's see it!", "Show what you got!")
        }

    val chaosLines: List<String>
        get() = when (LanguageManager.shared.currentLanguage) {
            AppLanguage.FRENCH -> listOf("Je te l'avais dit !", "Chaos total !", "Bonne chance !", "C'est parti !")
            AppLanguage.SPANISH -> listOf("¡Te lo advertí!", "¡Caos absoluto!", "Jaja, ¡buena suerte!", "¡¿Qué hemos hecho?!")
            AppLanguage.ENGLISH -> listOf("Told you so!", "Absolute chaos!", "Haha, good luck!", "What have we done?!")
        }
}
