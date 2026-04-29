package com.autoshorts.ai.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank
    private String workingDir;

    @NotNull
    private Cleanup cleanup = new Cleanup();

    @NotNull
    private Assets assets = new Assets();

    @NotNull
    private Ffmpeg ffmpeg = new Ffmpeg();

    @NotNull
    private Redis redis = new Redis();

    @NotNull
    private OpenAi openai = new OpenAi();

    @NotNull
    private Visual visual = new Visual();

    @NotNull
    private ElevenLabs elevenlabs = new ElevenLabs();

    @NotNull
    private Storage storage = new Storage();

    @NotNull
    private Scheduler scheduler = new Scheduler();

    @NotNull
    private Queue queue = new Queue();

    @NotNull
    private Publish publish = new Publish();

    @NotNull
    private TikTok tiktok = new TikTok();

    @NotNull
    private Webhook webhook = new Webhook();

    @NotNull
    private Web web = new Web();

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public Cleanup getCleanup() {
        return cleanup;
    }

    public void setCleanup(Cleanup cleanup) {
        this.cleanup = cleanup;
    }

    public Assets getAssets() {
        return assets;
    }

    public void setAssets(Assets assets) {
        this.assets = assets;
    }

    public Ffmpeg getFfmpeg() {
        return ffmpeg;
    }

    public void setFfmpeg(Ffmpeg ffmpeg) {
        this.ffmpeg = ffmpeg;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public Visual getVisual() {
        return visual;
    }

    public void setVisual(Visual visual) {
        this.visual = visual;
    }

    public ElevenLabs getElevenlabs() {
        return elevenlabs;
    }

    public void setElevenlabs(ElevenLabs elevenlabs) {
        this.elevenlabs = elevenlabs;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Queue getQueue() {
        return queue;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    public Publish getPublish() {
        return publish;
    }

    public void setPublish(Publish publish) {
        this.publish = publish;
    }

    public TikTok getTiktok() {
        return tiktok;
    }

    public void setTiktok(TikTok tiktok) {
        this.tiktok = tiktok;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    public Web getWeb() {
        return web;
    }

    public void setWeb(Web web) {
        this.web = web;
    }

    public static class Cleanup {

        private boolean deleteTempFiles = true;
        private boolean keepFailedJobFiles = true;

        public boolean isDeleteTempFiles() {
            return deleteTempFiles;
        }

        public void setDeleteTempFiles(boolean deleteTempFiles) {
            this.deleteTempFiles = deleteTempFiles;
        }

        public boolean isKeepFailedJobFiles() {
            return keepFailedJobFiles;
        }

        public void setKeepFailedJobFiles(boolean keepFailedJobFiles) {
            this.keepFailedJobFiles = keepFailedJobFiles;
        }
    }

    public static class Assets {

        @NotBlank
        private String defaultBackgroundPath;

        public String getDefaultBackgroundPath() {
            return defaultBackgroundPath;
        }

        public void setDefaultBackgroundPath(String defaultBackgroundPath) {
            this.defaultBackgroundPath = defaultBackgroundPath;
        }
    }

    public static class Ffmpeg {

        @NotBlank
        private String binary = "ffmpeg";

        private long timeoutSeconds = 180;

        public String getBinary() {
            return binary;
        }

        public void setBinary(String binary) {
            this.binary = binary;
        }

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Redis {

        private boolean cacheEnabled = true;
        private long cacheTtlSeconds = 21_600;

        public boolean isCacheEnabled() {
            return cacheEnabled;
        }

        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }

        public long getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(long cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }
    }

    public static class OpenAi {

        @NotBlank
        private String baseUrl = "http://localhost:20128/v1";

        private String apiKey;
        private String model = "if/qwen3-coder-plus";
        private String speechModel = "openai/tts-1";
        private String speechVoice = "alloy";
        private String speechResponseFormat = "mp3";
        @Min(1000)
        private long requestTimeoutMs = 60_000;
        @Min(1000)
        private long speechRequestTimeoutMs = 60_000;
        private boolean mock;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getSpeechModel() {
            return speechModel;
        }

        public void setSpeechModel(String speechModel) {
            this.speechModel = speechModel;
        }

        public String getSpeechVoice() {
            return speechVoice;
        }

        public void setSpeechVoice(String speechVoice) {
            this.speechVoice = speechVoice;
        }

        public String getSpeechResponseFormat() {
            return speechResponseFormat;
        }

        public void setSpeechResponseFormat(String speechResponseFormat) {
            this.speechResponseFormat = speechResponseFormat;
        }

        public long getRequestTimeoutMs() {
            return requestTimeoutMs;
        }

        public void setRequestTimeoutMs(long requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
        }

        public long getSpeechRequestTimeoutMs() {
            return speechRequestTimeoutMs;
        }

        public void setSpeechRequestTimeoutMs(long speechRequestTimeoutMs) {
            this.speechRequestTimeoutMs = speechRequestTimeoutMs;
        }

        public boolean isMock() {
            return mock;
        }

        public void setMock(boolean mock) {
            this.mock = mock;
        }
    }

    public static class Visual {

        private boolean enabled = true;
        private boolean mock = true;
        private String provider = "openai";
        private String model = "gpt-image-1";
        private String size = "1024x1536";

        @Min(1)
        @Max(12)
        private int maxScenes = 5;

        @Min(500)
        private long requestTimeoutMs = 60_000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isMock() {
            return mock;
        }

        public void setMock(boolean mock) {
            this.mock = mock;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public int getMaxScenes() {
            return maxScenes;
        }

        public void setMaxScenes(int maxScenes) {
            this.maxScenes = maxScenes;
        }

        public long getRequestTimeoutMs() {
            return requestTimeoutMs;
        }

        public void setRequestTimeoutMs(long requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
        }
    }

    public static class ElevenLabs {

        @NotBlank
        private String baseUrl = "https://api.elevenlabs.io";

        private String apiKey;
        private boolean enabled = true;
        private String defaultVoiceId;
        private String defaultModelId = "eleven_v3";
        private String outputFormat = "mp3_44100_128";
        private long requestTimeoutMs = 15_000;
        private boolean mock;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDefaultVoiceId() {
            return defaultVoiceId;
        }

        public void setDefaultVoiceId(String defaultVoiceId) {
            this.defaultVoiceId = defaultVoiceId;
        }

        public String getDefaultModelId() {
            return defaultModelId;
        }

        public void setDefaultModelId(String defaultModelId) {
            this.defaultModelId = defaultModelId;
        }

        public String getModelId() {
            return defaultModelId;
        }

        public void setModelId(String modelId) {
            this.defaultModelId = modelId;
        }

        public String getOutputFormat() {
            return outputFormat;
        }

        public void setOutputFormat(String outputFormat) {
            this.outputFormat = outputFormat;
        }

        public long getRequestTimeoutMs() {
            return requestTimeoutMs;
        }

        public void setRequestTimeoutMs(long requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
        }

        public boolean isMock() {
            return mock;
        }

        public void setMock(boolean mock) {
            this.mock = mock;
        }
    }

    public static class Storage {

        private boolean mock;
        private String region = "us-east-1";
        private String bucket = "autoshorts-assets";
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String publicBaseUrl;
        private String localPublicBaseUrl;
        private long presignDurationSeconds = 86_400;

        public boolean isMock() {
            return mock;
        }

        public void setMock(boolean mock) {
            this.mock = mock;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }

        public String getLocalPublicBaseUrl() {
            return localPublicBaseUrl;
        }

        public void setLocalPublicBaseUrl(String localPublicBaseUrl) {
            this.localPublicBaseUrl = localPublicBaseUrl;
        }

        public long getPresignDurationSeconds() {
            return presignDurationSeconds;
        }

        public void setPresignDurationSeconds(long presignDurationSeconds) {
            this.presignDurationSeconds = presignDurationSeconds;
        }
    }

    public static class Scheduler {

        private boolean enabled;

        @Min(1)
        private long fixedDelayMs = 60_000;

        @Min(0)
        private long initialDelayMs = 15_000;

        @Min(1)
        @Max(100)
        private int batchSize = 3;

        @Min(10)
        @Max(120)
        private int defaultDurationSeconds = 30;

        private String defaultVoiceId;
        private String topicSource = "scheduler";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getFixedDelayMs() {
            return fixedDelayMs;
        }

        public void setFixedDelayMs(long fixedDelayMs) {
            this.fixedDelayMs = fixedDelayMs;
        }

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getDefaultDurationSeconds() {
            return defaultDurationSeconds;
        }

        public void setDefaultDurationSeconds(int defaultDurationSeconds) {
            this.defaultDurationSeconds = defaultDurationSeconds;
        }

        public String getDefaultVoiceId() {
            return defaultVoiceId;
        }

        public void setDefaultVoiceId(String defaultVoiceId) {
            this.defaultVoiceId = defaultVoiceId;
        }

        public String getTopicSource() {
            return topicSource;
        }

        public void setTopicSource(String topicSource) {
            this.topicSource = topicSource;
        }
    }

    public static class Queue {

        private boolean enabled = true;

        @NotBlank
        private String exchange = "autoshorts.video.exchange";

        @NotBlank
        private String routingKey = "video.generate";

        @NotBlank
        private String queue = "autoshorts.video.generate";

        @NotBlank
        private String deadLetterExchange = "autoshorts.video.dlx";

        @NotBlank
        private String deadLetterRoutingKey = "video.generate.dlq";

        @NotBlank
        private String deadLetterQueue = "autoshorts.video.generate.dlq";

        @Min(1)
        @Max(10)
        private int consumerConcurrency = 2;

        @Min(1)
        @Max(10)
        private int maxProcessingAttempts = 3;

        @Min(100)
        private long retryInitialIntervalMs = 2_000;

        private double retryMultiplier = 2.0;

        @Min(100)
        private long retryMaxIntervalMs = 10_000;

        private boolean recoveryEnabled = true;

        @Min(1)
        private long stuckProcessingTimeoutMinutes = 30;

        @Min(1)
        private long pendingRedispatchDelayMinutes = 5;

        @Min(1)
        @Max(500)
        private int recoveryBatchSize = 50;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }

        public String getDeadLetterExchange() {
            return deadLetterExchange;
        }

        public void setDeadLetterExchange(String deadLetterExchange) {
            this.deadLetterExchange = deadLetterExchange;
        }

        public String getDeadLetterRoutingKey() {
            return deadLetterRoutingKey;
        }

        public void setDeadLetterRoutingKey(String deadLetterRoutingKey) {
            this.deadLetterRoutingKey = deadLetterRoutingKey;
        }

        public String getDeadLetterQueue() {
            return deadLetterQueue;
        }

        public void setDeadLetterQueue(String deadLetterQueue) {
            this.deadLetterQueue = deadLetterQueue;
        }

        public int getConsumerConcurrency() {
            return consumerConcurrency;
        }

        public void setConsumerConcurrency(int consumerConcurrency) {
            this.consumerConcurrency = consumerConcurrency;
        }

        public int getMaxProcessingAttempts() {
            return maxProcessingAttempts;
        }

        public void setMaxProcessingAttempts(int maxProcessingAttempts) {
            this.maxProcessingAttempts = maxProcessingAttempts;
        }

        public long getRetryInitialIntervalMs() {
            return retryInitialIntervalMs;
        }

        public void setRetryInitialIntervalMs(long retryInitialIntervalMs) {
            this.retryInitialIntervalMs = retryInitialIntervalMs;
        }

        public double getRetryMultiplier() {
            return retryMultiplier;
        }

        public void setRetryMultiplier(double retryMultiplier) {
            this.retryMultiplier = retryMultiplier;
        }

        public long getRetryMaxIntervalMs() {
            return retryMaxIntervalMs;
        }

        public void setRetryMaxIntervalMs(long retryMaxIntervalMs) {
            this.retryMaxIntervalMs = retryMaxIntervalMs;
        }

        public boolean isRecoveryEnabled() {
            return recoveryEnabled;
        }

        public void setRecoveryEnabled(boolean recoveryEnabled) {
            this.recoveryEnabled = recoveryEnabled;
        }

        public long getStuckProcessingTimeoutMinutes() {
            return stuckProcessingTimeoutMinutes;
        }

        public void setStuckProcessingTimeoutMinutes(long stuckProcessingTimeoutMinutes) {
            this.stuckProcessingTimeoutMinutes = stuckProcessingTimeoutMinutes;
        }

        public long getPendingRedispatchDelayMinutes() {
            return pendingRedispatchDelayMinutes;
        }

        public void setPendingRedispatchDelayMinutes(long pendingRedispatchDelayMinutes) {
            this.pendingRedispatchDelayMinutes = pendingRedispatchDelayMinutes;
        }

        public int getRecoveryBatchSize() {
            return recoveryBatchSize;
        }

        public void setRecoveryBatchSize(int recoveryBatchSize) {
            this.recoveryBatchSize = recoveryBatchSize;
        }
    }

    public static class Publish {

        private boolean enabled = true;

        @NotBlank
        private String provider = "mock";

        @NotBlank
        private String defaultPlatform = "mock-social";

        private String mockFailureKeyword = "[publish-fail]";

        @Min(0)
        private long mockLatencyMs = 50;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getDefaultPlatform() {
            return defaultPlatform;
        }

        public void setDefaultPlatform(String defaultPlatform) {
            this.defaultPlatform = defaultPlatform;
        }

        public String getMockFailureKeyword() {
            return mockFailureKeyword;
        }

        public void setMockFailureKeyword(String mockFailureKeyword) {
            this.mockFailureKeyword = mockFailureKeyword;
        }

        public long getMockLatencyMs() {
            return mockLatencyMs;
        }

        public void setMockLatencyMs(long mockLatencyMs) {
            this.mockLatencyMs = mockLatencyMs;
        }
    }

    public static class TikTok {

        private String clientKey;
        private String clientSecret;
        private String redirectUri;
        private String scopes = "user.info.basic,video.publish";
        private String webhookVerifyToken;
        private String apiBaseUrl = "https://open.tiktokapis.com";

        public String getClientKey() {
            return clientKey;
        }

        public void setClientKey(String clientKey) {
            this.clientKey = clientKey;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getScopes() {
            return scopes;
        }

        public void setScopes(String scopes) {
            this.scopes = scopes;
        }

        public String getWebhookVerifyToken() {
            return webhookVerifyToken;
        }

        public void setWebhookVerifyToken(String webhookVerifyToken) {
            this.webhookVerifyToken = webhookVerifyToken;
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }
    }

    public static class Webhook {

        private boolean enabled;
        private String endpointUrl;

        @Min(1)
        @Max(100)
        private int dispatchBatchSize = 20;

        @Min(100)
        private long dispatchFixedDelayMs = 5_000;

        @Min(1)
        @Max(20)
        private int maxAttempts = 5;

        @Min(100)
        private long retryInitialDelayMs = 1_000;

        private double retryMultiplier = 2.0;

        @Min(100)
        private long retryMaxDelayMs = 60_000;

        @Min(1)
        @Max(120)
        private int requestTimeoutSeconds = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpointUrl() {
            return endpointUrl;
        }

        public void setEndpointUrl(String endpointUrl) {
            this.endpointUrl = endpointUrl;
        }

        public int getDispatchBatchSize() {
            return dispatchBatchSize;
        }

        public void setDispatchBatchSize(int dispatchBatchSize) {
            this.dispatchBatchSize = dispatchBatchSize;
        }

        public long getDispatchFixedDelayMs() {
            return dispatchFixedDelayMs;
        }

        public void setDispatchFixedDelayMs(long dispatchFixedDelayMs) {
            this.dispatchFixedDelayMs = dispatchFixedDelayMs;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getRetryInitialDelayMs() {
            return retryInitialDelayMs;
        }

        public void setRetryInitialDelayMs(long retryInitialDelayMs) {
            this.retryInitialDelayMs = retryInitialDelayMs;
        }

        public double getRetryMultiplier() {
            return retryMultiplier;
        }

        public void setRetryMultiplier(double retryMultiplier) {
            this.retryMultiplier = retryMultiplier;
        }

        public long getRetryMaxDelayMs() {
            return retryMaxDelayMs;
        }

        public void setRetryMaxDelayMs(long retryMaxDelayMs) {
            this.retryMaxDelayMs = retryMaxDelayMs;
        }

        public int getRequestTimeoutSeconds() {
            return requestTimeoutSeconds;
        }

        public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
            this.requestTimeoutSeconds = requestTimeoutSeconds;
        }
    }

    public static class Web {

        @NotNull
        private Cors cors = new Cors();

        public Cors getCors() {
            return cors;
        }

        public void setCors(Cors cors) {
            this.cors = cors;
        }
    }

    public static class Cors {

        @NotNull
        private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");

        @NotNull
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

        @NotNull
        private List<String> allowedHeaders = List.of("*");

        private List<String> exposedHeaders = List.of("Location");

        private boolean allowCredentials = true;

        @Min(0)
        private long maxAgeSeconds = 3_600;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAgeSeconds() {
            return maxAgeSeconds;
        }

        public void setMaxAgeSeconds(long maxAgeSeconds) {
            this.maxAgeSeconds = maxAgeSeconds;
        }
    }
}
