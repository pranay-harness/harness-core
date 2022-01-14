package io.harness.enforcement.client.example;

import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ExampleRateLimitUsageImpl implements RestrictionUsageInterface<RateLimitRestrictionMetadataDTO> {
  @Override
  public long getCurrentValue(String accountIdentifier, RateLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    return 10;
  }
}
