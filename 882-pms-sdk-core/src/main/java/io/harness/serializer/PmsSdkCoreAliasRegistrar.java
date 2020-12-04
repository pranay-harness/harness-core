package io.harness.serializer;

import io.harness.pms.sdk.core.facilitator.DefaultFacilitatorParams;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

public class PmsSdkCoreAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("defaultFacilitatorParams", DefaultFacilitatorParams.class);
  }
}
