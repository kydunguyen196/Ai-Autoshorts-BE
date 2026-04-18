package com.autoshorts.ai.dto;

import java.util.List;

public class FrontendBootstrapResponse {

    private List<String> supportedStyles;
    private List<String> videoStatuses;
    private List<String> generationSteps;
    private List<String> topicStatuses;
    private FrontendDefaults defaults;

    public List<String> getSupportedStyles() {
        return supportedStyles;
    }

    public void setSupportedStyles(List<String> supportedStyles) {
        this.supportedStyles = supportedStyles;
    }

    public List<String> getVideoStatuses() {
        return videoStatuses;
    }

    public void setVideoStatuses(List<String> videoStatuses) {
        this.videoStatuses = videoStatuses;
    }

    public List<String> getGenerationSteps() {
        return generationSteps;
    }

    public void setGenerationSteps(List<String> generationSteps) {
        this.generationSteps = generationSteps;
    }

    public List<String> getTopicStatuses() {
        return topicStatuses;
    }

    public void setTopicStatuses(List<String> topicStatuses) {
        this.topicStatuses = topicStatuses;
    }

    public FrontendDefaults getDefaults() {
        return defaults;
    }

    public void setDefaults(FrontendDefaults defaults) {
        this.defaults = defaults;
    }

    public static class FrontendDefaults {

        private String defaultStyle;
        private int defaultDurationSeconds;
        private int minDurationSeconds;
        private int maxDurationSeconds;
        private int defaultVideosPageSize;
        private int maxVideosPageSize;
        private int defaultTopicsPageSize;
        private int maxTopicsPageSize;
        private String defaultVoiceId;

        public String getDefaultStyle() {
            return defaultStyle;
        }

        public void setDefaultStyle(String defaultStyle) {
            this.defaultStyle = defaultStyle;
        }

        public int getDefaultDurationSeconds() {
            return defaultDurationSeconds;
        }

        public void setDefaultDurationSeconds(int defaultDurationSeconds) {
            this.defaultDurationSeconds = defaultDurationSeconds;
        }

        public int getMinDurationSeconds() {
            return minDurationSeconds;
        }

        public void setMinDurationSeconds(int minDurationSeconds) {
            this.minDurationSeconds = minDurationSeconds;
        }

        public int getMaxDurationSeconds() {
            return maxDurationSeconds;
        }

        public void setMaxDurationSeconds(int maxDurationSeconds) {
            this.maxDurationSeconds = maxDurationSeconds;
        }

        public int getDefaultVideosPageSize() {
            return defaultVideosPageSize;
        }

        public void setDefaultVideosPageSize(int defaultVideosPageSize) {
            this.defaultVideosPageSize = defaultVideosPageSize;
        }

        public int getMaxVideosPageSize() {
            return maxVideosPageSize;
        }

        public void setMaxVideosPageSize(int maxVideosPageSize) {
            this.maxVideosPageSize = maxVideosPageSize;
        }

        public int getDefaultTopicsPageSize() {
            return defaultTopicsPageSize;
        }

        public void setDefaultTopicsPageSize(int defaultTopicsPageSize) {
            this.defaultTopicsPageSize = defaultTopicsPageSize;
        }

        public int getMaxTopicsPageSize() {
            return maxTopicsPageSize;
        }

        public void setMaxTopicsPageSize(int maxTopicsPageSize) {
            this.maxTopicsPageSize = maxTopicsPageSize;
        }

        public String getDefaultVoiceId() {
            return defaultVoiceId;
        }

        public void setDefaultVoiceId(String defaultVoiceId) {
            this.defaultVoiceId = defaultVoiceId;
        }
    }
}
