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

abstract class DifficultyConstants {
    abstract fun whatIsFuelAirRefillSpeed(): Float
    abstract fun whatIsFuelGroundRefillSpeed(): Float
    abstract fun whatIsMaxPlayerLife(): Int
    abstract fun whatIsCoinsPerPowerup(): Int
    abstract fun whatIsGlowDuration(): Float
    abstract fun whatIsDDAStage1Attempts(): Int
    abstract fun whatIsDDAStage2Attempts(): Int
    abstract fun whatIsDDAStage1LifeBoost(): Int
    abstract fun whatIsDDAStage2LifeBoost(): Int
    abstract fun whatIsDDAStage1FuelAirRefillSpeed(): Float
    abstract fun whatIsDDAStage2FuelAirRefillSpeed(): Float
}