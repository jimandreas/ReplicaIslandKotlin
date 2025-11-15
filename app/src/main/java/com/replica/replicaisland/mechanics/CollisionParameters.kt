package com.replica.replicaisland.mechanics

/**
 * CollisionParamaters defines global parameters related to dynamic (object vs object) collisions.
 */
class CollisionParameters {
    // HitType describes the type of hit that a victim object receives.  Victims may choose to
    // react differently to the intersection depending on the hit type.
    // TODO: Make this a bit field so that objects can support multiple hit types.
    object HitType {
        const val INVALID = 0 // No type.
        const val HIT = 1 // Standard hit type.  Life is reduced by 1.
        const val DEATH = 2 // Causes instant death.
        const val COLLECT = 3 // Causes collectable objects to be collected by the attacker.
        const val POSSESS = 4 // Causes possessable objects to become possessed.
        const val DEPRESS = 5 // A hit indicating that the attacker is pressing into the victim.
        const val LAUNCH = 6 // A hit indicating that the attacker will launch the victim.
    }
}