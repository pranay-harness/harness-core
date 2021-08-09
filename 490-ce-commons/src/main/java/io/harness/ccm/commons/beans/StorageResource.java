package io.harness.ccm.commons.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StorageResource {
  private Double capacity;
}
