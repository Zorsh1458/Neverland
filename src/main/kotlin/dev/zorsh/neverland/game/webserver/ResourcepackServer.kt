package dev.zorsh.neverland.game.webserver

import dev.zorsh.neverland.game.Neverland
import dev.zorsh.neverland.game.logger.Logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File
import kotlin.text.toIntOrNull

class ResourcepackServer {
    companion object {
        fun start(): NettyApplicationEngine? {
            val resourcepackFile = File("/resourcepack/Neverland.zip")

            val port = Neverland.config["web-port"]?.toIntOrNull()
            if (port == null) {
                Logger.error("Can't start webserver! Specify port in neverland.properties")
                return null
            }
            Logger.info("Starting webserver on port $port")
            return embeddedServer(Netty, port = port) {
                routing {
                    get("{...}") {
                        if (resourcepackFile.exists()) {
                            call.respondFile(resourcepackFile)
                        } else {
                            call.respondText("File not found", status = HttpStatusCode.NotFound)
                        }
                    }
                }
            }.start(wait = true)
        }
    }
}