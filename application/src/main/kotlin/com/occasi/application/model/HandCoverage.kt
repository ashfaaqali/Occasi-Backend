package com.occasi.application.model

enum class HandCoverage {
    FRONT, BACK, BOTH;

    fun getMultiplier(): Int = if (this == BOTH) 2 else 1
}
