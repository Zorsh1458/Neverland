package dev.zorsh.neverland.game.logger

class Logger {
    companion object {
        fun info(message: String) {
            println("[Neverland INFO]: $message")
        }

        fun warning(message: String) {
            println("[Neverland WARN]: $message")
        }

        fun error(message: String) {
            println("[Neverland ERROR]: $message")
        }
    }
}