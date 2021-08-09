package io.harness.azure.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSTagsData {
  String infraMappingId;
  Integer harnessRevision;
  boolean isBlueGreen;
}
