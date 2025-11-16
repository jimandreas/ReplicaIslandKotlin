package com.replica.replicaisland.utils

class KidsDifficultyConstants : DifficultyConstants() {
    override fun whatIsFuelAirRefillSpeed(): Float {
        return FUEL_AIR_REFILL_SPEED
    }

    override fun whatIsFuelGroundRefillSpeed(): Float {
        return FUEL_GROUND_REFILL_SPEED
    }

    override fun whatIsMaxPlayerLife(): Int {
        return MAX_PLAYER_LIFE
    }

    override fun whatIsCoinsPerPowerup(): Int {
        return COINS_PER_POWERUP
    }

    override fun whatIsGlowDuration(): Float {
        return GLOW_DURATION
    }

    override fun whatIsDDAStage1Attempts(): Int {
        return DDA_STAGE_1_ATTEMPTS
    }

    override fun whatIsDDAStage2Attempts(): Int {
        return DDA_STAGE_2_ATTEMPTS
    }

    override fun whatIsDDAStage1LifeBoost(): Int {
        return DDA_STAGE_1_LIFE_BOOST
    }

    override fun whatIsDDAStage2LifeBoost(): Int {
        return DDA_STAGE_2_LIFE_BOOST
    }

    override fun whatIsDDAStage1FuelAirRefillSpeed(): Float {
        return DDA_STAGE_1_FUEL_AIR_REFILL_SPEED
    }

    override fun whatIsDDAStage2FuelAirRefillSpeed(): Float {
        return DDA_STAGE_2_FUEL_AIR_REFILL_SPEED
    }

    companion object {
        private const val FUEL_AIR_REFILL_SPEED = 0.15f
        private const val FUEL_GROUND_REFILL_SPEED = 2.0f
        const val MAX_PLAYER_LIFE = 3
        private const val COINS_PER_POWERUP = 20
        const val GLOW_DURATION = 15.0f

        // DDA boosts
        private const val DDA_STAGE_1_ATTEMPTS = 3
        private const val DDA_STAGE_2_ATTEMPTS = 8
        private const val DDA_STAGE_1_LIFE_BOOST = 1
        private const val DDA_STAGE_2_LIFE_BOOST = 2
        private const val DDA_STAGE_1_FUEL_AIR_REFILL_SPEED = 0.22f
        private const val DDA_STAGE_2_FUEL_AIR_REFILL_SPEED = 0.30f
    }
}