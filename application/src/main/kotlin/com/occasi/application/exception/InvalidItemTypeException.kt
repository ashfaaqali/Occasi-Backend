package com.occasi.application.exception

class InvalidItemTypeException(value: String) : RuntimeException(
    "Invalid item type: '$value'. Must be ARTIST, DESIGN, or CARD"
)
