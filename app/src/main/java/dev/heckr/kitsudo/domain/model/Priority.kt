package dev.heckr.kitsudo.domain.model

enum class Priority(val dbValue: Int) {
    NORMAL(0),
    HIGH(1),
    ;

    companion object {
        fun fromDb(value: Int): Priority = entries.firstOrNull { it.dbValue == value } ?: NORMAL
    }
}
