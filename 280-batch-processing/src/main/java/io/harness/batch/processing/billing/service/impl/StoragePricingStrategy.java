package io.harness.batch.processing.billing.service.impl;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.pricing.data.StoragePricingData;
import io.harness.ccm.commons.entities.InstanceData;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StoragePricingStrategy implements InstancePricingStrategy {
  @Override
  public PricingData getPricePerHour(InstanceData instanceData, Instant startTime, Instant endTime,
      double instanceActiveSeconds, double parentInstanceActiveSecond) {
    Double pricePerMbPerHour = StoragePricingData.getPricePerMbPerHour(instanceData.getMetaData());
    Double storageMb = instanceData.getStorageResource().getCapacity();

    return PricingData.builder()
        .storageMb(storageMb)
        .pricePerHour(storageMb * pricePerMbPerHour)
        .pricingSource(StoragePricingData.getPricingSource(instanceData.getMetaData()))
        .networkCost(0D) // Google doesn't charge for network usage
        .build();
  }
}
