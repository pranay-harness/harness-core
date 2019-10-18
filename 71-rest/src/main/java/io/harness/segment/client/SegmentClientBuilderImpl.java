package io.harness.segment.client;

import com.google.inject.Singleton;

import com.segment.analytics.Analytics;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SegmentClientBuilderImpl implements SegmentClientBuilder {
  private final String writeKey;

  public SegmentClientBuilderImpl(String writeKey) {
    this.writeKey = writeKey;
  }

  private Analytics analytics;

  @Override
  public Analytics getInstance() {
    if (null != analytics) {
      return analytics;
    }

    this.analytics = Analytics.builder(this.writeKey).build();
    return this.analytics;
  }
}
