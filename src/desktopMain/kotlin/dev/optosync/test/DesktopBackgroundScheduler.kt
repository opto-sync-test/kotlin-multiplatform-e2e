package dev.optosync.test

import java.util.concurrent.Callable
import java.util.concurrent.Executors

class DesktopBackgroundScheduler(
    private val worker: MultiplexBackgroundWorker,
    parallelism: Int = 4,
) : AutoCloseable {
    private val executor = Executors.newFixedThreadPool(parallelism)

    fun drain(pending: List<Mutation>): List<LaneResult> {
        val futures = worker.snapshot(pending).map { batch ->
            executor.submit(Callable { worker.drainLane(batch) })
        }
        return futures.map { it.get() }.sortedBy(LaneResult::lane)
    }

    override fun close() {
        executor.shutdownNow()
    }
}
