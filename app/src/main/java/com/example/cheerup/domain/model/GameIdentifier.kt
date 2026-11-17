package com.example.cheerup.domain.model

import com.example.cheerup.core.localization.LanguageManager

enum class GameIdentifier(val rawValue: String) {
    TAP("tap"),
    PING_PONG("pingPong"),
    GRAVITY("gravity"),
    RUSH("rush"),
    CHESS("chess"),
    WHEELS("wheels");

    val id: String get() = rawValue

    val title: String
        get() = when (this) {
            TAP -> "TAP"
            PING_PONG -> "PING PONG"
            GRAVITY -> "GRAVITY"
            RUSH -> "RUSH"
            CHESS -> "CHESS"
            WHEELS -> "WHEELS"
        }

    val subtitle: String
        get() = when (this) {
            TAP -> LanguageManager.shared.tr("Reflex • Fast Taps", "Réflexes • Vitesse", "Reflejos • Velocidad")
            PING_PONG -> LanguageManager.shared.tr("Arcade • Chaos Rallies", "Arcade • Échanges Chaos", "Arcade • Rallies Caos")
            GRAVITY -> LanguageManager.shared.tr("Serpent • Shifting World", "Serpent • Monde Mouvant", "Serpiente • Mundo Cambiante")
            RUSH -> LanguageManager.shared.tr("Starship • Meteor Dodge", "Vaisseau • Météores", "Nave • Esquiva Meteoros")
            CHESS -> LanguageManager.shared.tr("Classic • Pixel Strategy", "Classique • Stratégie Pixel", "Clásico • Estrategia Píxel")
            WHEELS -> LanguageManager.shared.tr("Surprise Wheel", "Roue Surprise", "Rueda Sorpresa")
        }

    val shortDescription: String
        get() = when (this) {
            TAP -> LanguageManager.shared.tr(
                "Tap the right color before time runs out.",
                "Touchez la bonne couleur à temps.",
                "Toca el color correcto a tiempo."
            )
            PING_PONG -> LanguageManager.shared.tr(
                "Fast paced pixel table tennis with wild power ups and endless rallies.",
                "Ping pong pixel ultra rapide avec bonus fous et échanges sans fin.",
                "Ping pong píxel rápido con potenciadores locos y rallies sin fin."
            )
            GRAVITY -> LanguageManager.shared.tr(
                "Guide an enchanted pixel serpent across a torus world where crossing edges shifts gravity.",
                "Guidez un serpent pixel dans un monde torique où franchir les bords change la gravité.",
                "Guía a una serpiente píxel en un mundo toroidal donde cruzar bordes cambia la gravedad."
            )
            RUSH -> LanguageManager.shared.tr(
                "Pilot a tiny pixel starship through endless meteors. Survive longer to score higher!",
                "Pilotez un mini vaisseau pixel à travers les météores. Survivez plus longtemps pour marquer plus !",
                "Pilota una pequeña nave píxel entre meteoros. ¡Sobrevive más tiempo para sumar puntos!"
            )
            CHESS -> LanguageManager.shared.tr(
                "Play traditional pixel-art chess against three AI levels or race the 3 and 5 minute clocks.",
                "Jouez aux échecs pixel traditionnels contre trois niveaux d'IA ou défiez les horloges de 3 et 5 min.",
                "Juega ajedrez píxel tradicional contra tres niveles de IA o reta los relojes de 3 y 5 minutos."
            )
            WHEELS -> LanguageManager.shared.tr(
                "Spin the wheel for fun surprises and quick challenges.",
                "Tournez la roue pour des surprises et défis.",
                "Gira la rueda para sorpresas y retos."
            )
        }

    val symbol: String
        get() = when (this) {
            TAP -> "hand.tap.fill"
            PING_PONG -> "figure.table.tennis"
            GRAVITY -> "sparkles"
            RUSH -> "airplane"
            CHESS -> "crown.fill"
            WHEELS -> "slider.horizontal.3"
        }

    val isGame: Boolean
        get() = when (this) {
            TAP, PING_PONG, GRAVITY, RUSH, CHESS -> true
            WHEELS -> false
        }
}
