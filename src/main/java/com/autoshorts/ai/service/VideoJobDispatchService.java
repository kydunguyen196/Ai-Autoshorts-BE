package com.autoshorts.ai.service;

import com.autoshorts.ai.entity.GenerationStep;
import com.autoshorts.ai.queue.VideoJobQueuePublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VideoJobDispatchService {

    private final VideoJobQueuePublisher videoJobQueuePublisher;
    private final VideoJobService videoJobService;

    public VideoJobDispatchService(VideoJobQueuePublisher videoJobQueuePublisher, VideoJobService videoJobService) {
        this.videoJobQueuePublisher = videoJobQueuePublisher;
        this.videoJobService = videoJobService;
    }

    public void publishOrMarkFailed(UUID jobId, String dispatchSource, String failureContext) {
        try {
            videoJobQueuePublisher.publish(jobId, dispatchSource);
        } catch (Exception ex) {
            videoJobService.markFailed(
                jobId,
                GenerationStep.QUEUED,
                failureContext,
                ex.getMessage()
            );
            throw ex;
        }
    }
}
