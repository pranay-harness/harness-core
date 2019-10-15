package io.harness.batch.processing.billing.service.intfc;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.entities.InstanceData;

public interface InstancePricingStrategy { PricingData getPricePerHour(InstanceData instanceData); }
