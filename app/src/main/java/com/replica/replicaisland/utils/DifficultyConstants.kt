package com.replica.replicaisland.utils

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