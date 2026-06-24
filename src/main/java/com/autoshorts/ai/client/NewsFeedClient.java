package com.autoshorts.ai.client;

import java.time.Instant;
import java.util.List;

/**
 * Fetches entries from an external news feed (e.g. RSS/Atom). Implementations are
 * selected/wired like the other {@code client/} abstractions.
 */
public interface NewsFeedClient {

    List<NewsFeedEntry> fetch(String feedUrl, int maxItems);

    record NewsFeedEntry(
        String guid,
        String title,
        String link,
        String summary,
        Instant publishedAt
    ) {
    }
}
