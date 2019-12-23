package io.harness.batch.processing.pricing.aws.athena;

import io.harness.batch.processing.pricing.data.AccountComputePricingData;
import io.harness.batch.processing.pricing.data.AccountFargatePricingData;

import java.time.Instant;
import java.util.List;

public interface AwsAthenaQueryHelperService {
  List<AccountComputePricingData> fetchComputePriceRate(String billingAccountId, Instant startDate)
      throws InterruptedException;

  List<AccountFargatePricingData> fetchEcsFargatePriceRate(String billingAccountId, Instant startInstant)
      throws InterruptedException;
}
