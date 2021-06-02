package io.harness.azure.model.blueprint;

import io.harness.azure.model.blueprint.vault.SecretValueReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParameterValue {
  private Object value;
  private SecretValueReference reference;
}
