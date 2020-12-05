package io.harness.serializer;

import io.harness.pms.sdk.core.adviser.fail.OnFailAdviserParameters;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviserParameters;
import io.harness.pms.sdk.core.adviser.retry.RetryAdviserParameters;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.facilitator.DefaultFacilitatorParams;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

public class PmsSdkCoreAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("defaultFacilitatorParams", DefaultFacilitatorParams.class);
    orchestrationElements.put("onFailAdviserParameters", OnFailAdviserParameters.class);
    orchestrationElements.put("ignoreAdviserParameters", IgnoreAdviserParameters.class);
    orchestrationElements.put("onSuccessAdviserParameters", OnSuccessAdviserParameters.class);
    orchestrationElements.put("retryAdviserParameters", RetryAdviserParameters.class);
  }
}
