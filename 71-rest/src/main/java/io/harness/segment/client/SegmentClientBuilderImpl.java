package io.harness.segment.client;

import com.segment.analytics.Analytics;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    // log added for debugging purpose, will be removed
    logger.info("Segment API key: {}", this.writeKey);

    this.analytics = Analytics.builder(this.writeKey).build();
    return this.analytics;
  }
}
