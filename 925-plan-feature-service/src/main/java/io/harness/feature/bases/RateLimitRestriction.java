package io.harness.feature.bases;

import io.harness.feature.beans.TimeUnit;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.interfaces.LimitRestriction;
import io.harness.feature.services.impl.FeatureUsageClient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitRestriction extends Restriction implements LimitRestriction {
  Long limit;
  TimeUnit timeUnit;
  String clientName;
  FeatureUsageClient featureUsageClient;

  public RateLimitRestriction(
      RestrictionType restrictionType, long limit, TimeUnit timeUnit, FeatureUsageClient featureUsageClient) {
    super(restrictionType);
    this.limit = limit;
    this.timeUnit = timeUnit;
    this.featureUsageClient = featureUsageClient;
  }

  public void setFeatureUsageClient(FeatureUsageClient featureUsageClient) {
    this.featureUsageClient = featureUsageClient;
  }
}
