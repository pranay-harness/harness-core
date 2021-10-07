package io.harness.enforcement.client.usage;

import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;

public interface RestrictionUsageInterface<T extends RestrictionMetadataDTO> {
  long getCurrentValue(String accountIdentifier, T restrictionMetadataDTO);
}
