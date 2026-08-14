package dev.optosync.test

data class Mutation(
    val lane: String,
    val sequence: Long,
    val recordId: String,
    val json: String,
)

data class ImmutableBatch(
    val lane: String,
    val mutations: List<Mutation>,
)

data class LaneResult(
    val lane: String,
    val acknowledgedThrough: Long,
    val authoritativeJson: List<String>,
    val attempts: Int,
)

fun interface BatchTransport {
    fun exchange(batch: ImmutableBatch): List<String>
}

class MultiplexBackgroundWorker(
    private val transport: BatchTransport,
    private val maxAttempts: Int = 3,
) {
    init {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
    }

    fun snapshot(pending: List<Mutation>): List<ImmutableBatch> =
        pending
            .groupBy(Mutation::lane)
            .map { (lane, mutations) ->
                ImmutableBatch(lane, mutations.sortedBy(Mutation::sequence).toList())
            }
            .sortedBy(ImmutableBatch::lane)

    fun drainLane(batch: ImmutableBatch): LaneResult {
        require(batch.mutations.isNotEmpty()) { "A background batch must not be empty" }
        require(batch.mutations.all { it.lane == batch.lane }) { "A batch may contain only one lane" }

        var failure: Throwable? = null
        repeat(maxAttempts) { attempt ->
            try {
                val authoritative = transport.exchange(batch)
                return LaneResult(
                    lane = batch.lane,
                    acknowledgedThrough = batch.mutations.last().sequence,
                    authoritativeJson = authoritative,
                    attempts = attempt + 1,
                )
            } catch (error: Throwable) {
                failure = error
            }
        }

        throw IllegalStateException("OptoSync lane ${batch.lane} exhausted its replay budget", failure)
    }
}
