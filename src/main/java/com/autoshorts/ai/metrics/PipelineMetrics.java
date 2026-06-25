package com.autoshorts.ai.metrics;

import com.autoshorts.ai.entity.GenerationStep;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Centralized Micrometer instrumentation for the video-generation pipeline.
 * Exposed via the Prometheus actuator endpoint; reused by the orchestrator and the queue consumer.
 */
@Component
public class PipelineMetrics {

    private static final String STEP_TIMER = "autoshorts.pipeline.step";
    private static final String JOB_COUNTER = "autoshorts.pipeline.job";
    private static final String QUEUE_COUNTER = "autoshorts.queue.consumed";

    private final MeterRegistry registry;

    public PipelineMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Records the duration and outcome ("success"/"failure") of a single pipeline step. */
    public void recordStep(GenerationStep step, String outcome, long elapsedMs) {
        registry.timer(STEP_TIMER, "step", safe(step), "outcome", outcome)
            .record(elapsedMs, TimeUnit.MILLISECONDS);
    }

    /** Records a terminal job outcome ("completed"/"failed"); failedStep is "none" on success. */
    public void recordJobOutcome(String outcome, GenerationStep failedStep) {
        registry.counter(JOB_COUNTER, "outcome", outcome, "failed_step", safe(failedStep))
            .increment();
    }

    /** Records the result of consuming a queue message ("processed"/"skipped"/"error"). */
    public void recordConsumed(String result) {
        registry.counter(QUEUE_COUNTER, "result", result).increment();
    }

    /** Records a message that exhausted retries and was dead-lettered. */
    public void recordDeadLettered() {
        registry.counter(QUEUE_COUNTER, "result", "dead_lettered").increment();
    }

    private String safe(GenerationStep step) {
        return step == null ? "none" : step.name();
    }
}
