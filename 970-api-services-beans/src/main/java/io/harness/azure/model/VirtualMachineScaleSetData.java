package io.harness.azure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualMachineScaleSetData {
  private String id;
  private String name;
  private String virtualMachineAdministratorUsername;
}
