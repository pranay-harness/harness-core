package io.harness.batch.processing.pricing.aws.athena;

import io.harness.batch.processing.pricing.data.AccountComputePricingData;
import io.harness.batch.processing.pricing.data.AccountFargatePricingData;

import java.time.Instant;
import java.util.List;

public interface AwsAthenaQueryHelperService {
  List<AccountComputePricingData> fetchComputePriceRate(String settingId, String billingAccountId, Instant startDate)
      throws InterruptedException;

  List<AccountFargatePricingData> fetchEcsFargatePriceRate(
      String settingId, String billingAccountId, Instant startInstant) throws InterruptedException;
}
