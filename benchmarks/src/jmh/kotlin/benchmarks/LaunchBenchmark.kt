/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package benchmarks

import kotlinx.coroutines.experimental.launch
import org.openjdk.jmh.annotations.*
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

/*
 * Benchmark to measure scheduling overhead in comparison with FJP.
 * LaunchBenchmark.massiveLaunch  experimental  avgt   30  328.662 ± 52.789  us/op
 * LaunchBenchmark.massiveLaunch           fjp  avgt   30  179.762 ±  3.931  us/op
 */
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
open class LaunchBenchmark : ParametrizedDispatcherBase() {

    @Param("experimental", "fjp")
    override var dispatcher: String = "fjp"

    private val jobsToLaunch = 100
    private val submitters = 4

    private val allLaunched = CyclicBarrier(submitters)
    private val stopBarrier = CyclicBarrier(submitters + 1)

    @Benchmark
    fun massiveLaunch() {
        repeat(submitters) {
            launch(benchmarkContext) {
                // Wait until all cores are occupied
                allLaunched.await()
                allLaunched.reset()

                (1..jobsToLaunch).map {
                    launch(coroutineContext) {
                        // do nothing
                    }
                }.map { it.join() }

                stopBarrier.await()
            }
        }

        stopBarrier.await()
        stopBarrier.reset()
    }

}