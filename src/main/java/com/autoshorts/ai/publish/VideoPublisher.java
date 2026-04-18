package com.autoshorts.ai.publish;

public interface VideoPublisher {

    String providerKey();

    PublishResult publish(PublishRequest request);
}
