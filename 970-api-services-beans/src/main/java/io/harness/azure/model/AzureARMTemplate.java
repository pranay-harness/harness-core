package io.harness.azure.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureARMTemplate {
  private String deploymentName;
  private String templateJSON;
  private String parametersJSON;
  private String location;
  private AzureDeploymentMode deploymentMode;
}
