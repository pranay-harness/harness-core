package io.harness.azure.model.blueprint.assignment.operation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureResourceManagerError {
  private String code;
  private String message;
}
