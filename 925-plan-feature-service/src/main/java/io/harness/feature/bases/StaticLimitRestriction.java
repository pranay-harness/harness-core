package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;
import io.harness.feature.interfaces.LimitRestriction;
import io.harness.feature.services.impl.FeatureUsageClient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StaticLimitRestriction extends Restriction implements LimitRestriction {
  Long limit;
  String clientName;
  FeatureUsageClient featureUsageClient;

  public StaticLimitRestriction(RestrictionType restrictionType, long limit, FeatureUsageClient featureUsageClient) {
    super(restrictionType);
    this.limit = limit;
    this.featureUsageClient = featureUsageClient;
  }

  public void setFeatureUsageClient(FeatureUsageClient featureUsageClient) {
    this.featureUsageClient = featureUsageClient;
  }
}