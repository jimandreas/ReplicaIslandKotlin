/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.replica.replicaisland

class AdultsDifficultyConstants : DifficultyConstants() {
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
        const val MAX_PLAYER_LIFE = 2
        private const val COINS_PER_POWERUP = 30
        const val GLOW_DURATION = 10.0f

        // DDA boosts
        private const val DDA_STAGE_1_ATTEMPTS = 4
        private const val DDA_STAGE_2_ATTEMPTS = 8
        private const val DDA_STAGE_1_LIFE_BOOST = 1
        private const val DDA_STAGE_2_LIFE_BOOST = 2
        private const val DDA_STAGE_1_FUEL_AIR_REFILL_SPEED = 0.15f
        private const val DDA_STAGE_2_FUEL_AIR_REFILL_SPEED = 0.22f
    }
}