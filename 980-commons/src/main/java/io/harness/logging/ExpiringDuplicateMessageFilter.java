/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.logging;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class ExpiringDuplicateMessageFilter extends TurboFilter {
  private static final int MAX_KEY_LENGTH = 100;
  private static final int DEFAULT_CACHE_SIZE = 3600;
  private static final int DEFAULT_ALLOWED_REPETITIONS = 5;
  private static final int DEFAULT_EXPIRE_AFTER_WRITE_SECONDS = 60;

  private int allowedRepetitions = DEFAULT_ALLOWED_REPETITIONS;
  private int cacheSize = DEFAULT_CACHE_SIZE;
  private int expireAfterWriteSeconds = DEFAULT_EXPIRE_AFTER_WRITE_SECONDS;
  private String includeMarkers = "";

  private List<Marker> includeMarkersList = new ArrayList<>();

  private Cache<String, Integer> msgCache;

  @Override
  public void start() {
    msgCache = buildCache();
    includeMarkersList = includeMarkers(includeMarkers);

    super.start();
  }

  @Override
  public void stop() {
    msgCache.invalidateAll();
    msgCache = null;

    super.stop();
  }

  @Override
  public FilterReply decide(final Marker marker, final Logger logger, final Level level, final String format,
      final Object[] params, final Throwable t) {
    if (!includeMarkersList.contains(marker)) {
      return FilterReply.NEUTRAL;
    }

    int count = 0;

    if (isNotBlank(format)) {
      final String key = abbreviate(format + paramsAsString(params, logger), MAX_KEY_LENGTH);

      final Integer msgCount = msgCache.getIfPresent(key);

      if (msgCount != null) {
        count = msgCount + 1;
      }

      msgCache.put(key, count);
    }

    return (count <= allowedRepetitions) ? FilterReply.NEUTRAL : FilterReply.DENY;
  }

  public int getAllowedRepetitions() {
    return allowedRepetitions;
  }

  public void setAllowedRepetitions(final int allowedRepetitions) {
    this.allowedRepetitions = allowedRepetitions;
  }

  public int getCacheSize() {
    return cacheSize;
  }

  public void setCacheSize(final int cacheSize) {
    this.cacheSize = cacheSize;
  }

  public int getExpireAfterWriteSeconds() {
    return expireAfterWriteSeconds;
  }

  public void setExpireAfterWriteSeconds(final int expireAfterWriteSeconds) {
    this.expireAfterWriteSeconds = expireAfterWriteSeconds;
  }

  public String getIncludeMarkers() {
    return includeMarkers;
  }

  public void setIncludeMarkers(final String includeMarkers) {
    this.includeMarkers = includeMarkers;
  }

  private List<Marker> includeMarkers(final String markersToExclude) {
    final List<String> listOfMarkers = Arrays.asList(markersToExclude.split("\\s*,\\s*"));
    return listOfMarkers.stream().map(MarkerFactory::getMarker).collect(toList());
  }

  private String paramsAsString(final Object[] params, final Logger log) {
    if (params != null && startsWith(log.getName(), "com.example")) {
      return Arrays.stream(params).map(Object::toString).collect(joining("_"));
    }

    return "";
  }

  private Cache<String, Integer> buildCache() {
    return Caffeine.newBuilder()
        .expireAfterWrite(expireAfterWriteSeconds, TimeUnit.SECONDS)
        .initialCapacity(cacheSize)
        .maximumSize(cacheSize)
        .build();
  }
}
