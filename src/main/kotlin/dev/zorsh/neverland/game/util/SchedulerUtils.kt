package dev.zorsh.neverland.game.util

import net.minestom.server.MinecraftServer
import net.minestom.server.timer.TaskSchedule
import java.util.function.Supplier

fun runLater(delay: Int, action: () -> Unit) {
    MinecraftServer.getSchedulerManager().scheduleTask(action, TaskSchedule.tick(delay), TaskSchedule.stop())
}

fun runSync(action: () -> Unit) {
    MinecraftServer.getSchedulerManager().scheduleTask(action, TaskSchedule.immediate(), TaskSchedule.stop())
}

class CancellableTask(
    private val count: Int,
    private val action: (index: Int, task: CancellableTask) -> Unit
) : Supplier<TaskSchedule> {

    @Volatile
    private var cancelled = false

    private var counter = 0

    fun cancel() {
        cancelled = true
    }

    override fun get(): TaskSchedule {
        if (cancelled || counter >= count) {
            return TaskSchedule.stop()
        }
        try {
            action(counter, this)
        } catch (_: Exception) {}
        counter++
        return TaskSchedule.tick(1)
    }
}

class CancellableInfiniteTask(
    private val period: Int,
    private val action: (index: Int, task: CancellableInfiniteTask) -> Unit
) : Supplier<TaskSchedule> {

    @Volatile
    private var cancelled = false

    private var counter = 0

    fun cancel() {
        cancelled = true
    }

    override fun get(): TaskSchedule {
        if (cancelled) {
            return TaskSchedule.stop()
        }
        try {
            action(counter, this)
        } catch (_: Exception) {}
        counter++
        return TaskSchedule.tick(period)
    }
}

fun runRepeating(count: Int, action: (Int, CancellableTask) -> Unit) {
    MinecraftServer.getSchedulerManager().submitTask(CancellableTask(count, action))
}

fun runRepeating(count: Int, action: (Int) -> Unit) {
    runRepeating(count) { i, _ -> action(i) }
}

fun runInfinite(period: Int, action: (Int, CancellableInfiniteTask) -> Unit) {
    MinecraftServer.getSchedulerManager().submitTask(CancellableInfiniteTask(period, action))
}