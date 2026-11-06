package dev.zorsh.neverland.launcher

import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.lang.ProcessBuilder.Redirect
import java.util.Scanner
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

val isRunning = AtomicBoolean(true)

val activeProcessesMap = ConcurrentHashMap<String, Process>()
val serverWriters = ConcurrentHashMap<String, BufferedWriter>()

fun main() {
    println("[Launcher] Инициализация запуска сервера...")

    Runtime.getRuntime().addShutdownHook(Thread {
        println("\n[Launcher] Получен сигнал остановки лаунчера! Отключаю авто-рестарт...")
        isRunning.set(false)

        for (process in activeProcessesMap.values) {
            if (process.isAlive) {
                process.destroy()
            }
        }
        println("[Launcher] Все подпроцессы серверов остановлены.")
    })

    val lobbyThread = Thread { runServerLoop("server-lobby") }
    val gameThread = Thread { runServerLoop("server-game") }

    lobbyThread.start()
    gameThread.start()

    val scanner = Scanner(System.`in`)
    while (isRunning.get()) {
        try {
            if (!scanner.hasNextLine()) break

            val line = scanner.nextLine().trim()
            if (line.isEmpty()) continue

            if (line.equals("stop", ignoreCase = true)) {
                println("[Launcher] Инициировано ручное завершение работы...")
                isRunning.set(false)
                for (process in activeProcessesMap.values) {
                    if (process.isAlive) process.destroy()
                }
                break
            }

            val spaceIndex = line.indexOf(' ')
            if (spaceIndex == -1) {
                println("[Launcher] Неизвестный формат. Используйте 'stop', 'lobby <команда>' или 'game <команда>'")
                continue
            }

            val target = line.substring(0, spaceIndex).lowercase()
            val command = line.substring(spaceIndex + 1)

            val writer = serverWriters["server-$target"]
            if (writer != null) {
                if (command == "restart") {
                    println("[Launcher] Перезапуск '$target'...")
                    writer.write("restart\n")
                    writer.flush()

                    Thread.sleep(2000)
                    activeProcessesMap["server-$target"]?.destroy()
                } else {
                    writer.write(command + "\n")
                    writer.flush()
                    println("[Launcher -> ($target): $command")
                }
            } else {
                println("[Launcher] Сервер '$target' не найден или сейчас оффлайн.")
            }

        } catch (e: Exception) {
            if (isRunning.get()) {
                System.err.println("[Launcher] Ошибка ввода: ${e.message}")
            }
        }
    }

    lobbyThread.join()
    gameThread.join()

    println("[Launcher] Серверы завершили свою работу")
}

fun runServerLoop(filename: String) {
    val currentDir = File(".")
    val serverJar = File("$filename.jar")

    if (!serverJar.exists()) {
        System.err.println("[Launcher-$filename] КРИТИЧЕСКАЯ ОШИБКА: Файл ${serverJar.absolutePath} не найден!")
        return
    }

    while (isRunning.get()) {
        println("[Launcher-$filename] Запуск сервера...")

        try {
            val command = listOf(
                "java",
                "-Xms1G",
                "-Xmx1G",
                "-Dfile.encoding=UTF-8",
                "-jar",
                serverJar.absolutePath
            )

            val processBuilder = ProcessBuilder(command)
                .directory(currentDir)
                .redirectInput(Redirect.PIPE)
                .redirectOutput(Redirect.INHERIT)
                .redirectError(Redirect.INHERIT)

            val process = processBuilder.start()
            activeProcessesMap[filename] = process

            val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
            serverWriters[filename] = writer

            val exitCode = process.waitFor()

            serverWriters.remove(filename)
            activeProcessesMap.remove(filename)

            println("[Launcher-$filename] Сервер остановился с кодом: $exitCode")

            if (!isRunning.get()) {
                break
            }

            println("[Launcher-$filename] Перезапуск через 5 секунд... (Для отмены закройте лаунчер)")
            Thread.sleep(5000)
        } catch (e: Exception) {
            System.err.println("[Launcher-$filename] Ошибка при запуске подпроцесса:")
            e.printStackTrace()
            break
        }
    }

    println("[Launcher-$filename] Поток сервера успешно остановлен.")
}