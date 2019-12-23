package io.harness.batch.processing.billing.service.intfc;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.entities.InstanceData;

import java.time.Instant;

public interface InstancePricingStrategy { PricingData getPricePerHour(InstanceData instanceData, Instant startTime); }
