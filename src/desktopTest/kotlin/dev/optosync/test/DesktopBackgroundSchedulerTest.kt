package dev.optosync.test

import io.zedpkg.opto_sync.OptoSyncClient
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopBackgroundSchedulerTest {
    @Test
    fun replaysImmutableLanesConcurrentlyAfterReconnect() {
        val firstAttemptsEntered = CountDownLatch(2)
        val attempts = ConcurrentHashMap<String, AtomicInteger>()
        val observed = ConcurrentHashMap<String, CopyOnWriteArrayList<ImmutableBatch>>()
        val transport = BatchTransport { batch ->
            observed.computeIfAbsent(batch.lane) { CopyOnWriteArrayList() }.add(batch)
            val attempt = attempts.computeIfAbsent(batch.lane) { AtomicInteger() }.incrementAndGet()
            if (attempt == 1) {
                firstAttemptsEntered.countDown()
                assertTrue(firstAttemptsEntered.await(2, TimeUnit.SECONDS), "lanes did not overlap")
                throw IOException("simulated offline transport")
            }
            listOf("""{"lane":"${batch.lane}","state":"authoritative"}""")
        }

        val worker = MultiplexBackgroundWorker(transport)
        val pending = listOf(
            Mutation("desktop", 1, "d-1", "{}"),
            Mutation("mobile", 1, "m-1", "{}"),
            Mutation("desktop", 2, "d-2", "{}"),
            Mutation("mobile", 2, "m-2", "{}"),
        )

        DesktopBackgroundScheduler(worker, parallelism = 2).use { scheduler ->
            val results = scheduler.drain(pending)
            assertEquals(listOf("desktop", "mobile"), results.map(LaneResult::lane))
            assertTrue(results.all { it.attempts == 2 && it.acknowledgedThrough == 2L })
        }

        for (lane in listOf("desktop", "mobile")) {
            val snapshots = observed.getValue(lane)
            assertEquals(2, snapshots.size)
            assertEquals(snapshots[0], snapshots[1], "retry must reuse the immutable batch")
        }

        val officialClient = OptoSyncClient(URI("https://sync.example.test"), "test-token")
        assertEquals("sync.example.test", officialClient.baseUri.host)
        assertEquals("test-token", officialClient.bearerToken)
    }
}
