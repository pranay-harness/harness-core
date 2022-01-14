package io.harness.batch.processing.pricing;

import io.harness.ccm.commons.entities.batch.InstanceData;

import java.time.Instant;

public interface InstancePricingStrategy {
  PricingData getPricePerHour(InstanceData instanceData, Instant startTime, Instant endTime,
      double instanceActiveSeconds, double parentInstanceActiveSecond);
}
