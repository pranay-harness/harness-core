package io.harness.segment.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.segment.analytics.Analytics;
import software.wings.app.MainConfiguration;

@Singleton
public class SegmentClientBuilder {
  @Inject private MainConfiguration mainConfiguration;

  private Analytics analytics;

  public Analytics getInstance() {
    if (null != analytics) {
      return analytics;
    }

    String writeKey = mainConfiguration.getSegmentConfig().getApiKey();
    this.analytics = Analytics.builder(writeKey).build();
    return this.analytics;
  }
}
