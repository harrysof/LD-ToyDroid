package com.ldtoypad.remote.core.result

sealed class AppError : Throwable() {
    data class Network(override val message: String) : AppError()
    data class Server(val code: Int, override val message: String) : AppError()
    data class Parsing(override val message: String) : AppError()
    data class State(override val message: String) : AppError()
    data class Unknown(override val cause: Throwable?) : AppError()
}
