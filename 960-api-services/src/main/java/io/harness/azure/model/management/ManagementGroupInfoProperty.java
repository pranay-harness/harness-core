package io.harness.azure.model.management;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManagementGroupInfoProperty {
  private String displayName;
  private String tenantId;
}
