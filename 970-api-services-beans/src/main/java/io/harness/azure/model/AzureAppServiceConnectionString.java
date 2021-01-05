package io.harness.azure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureAppServiceConnectionString {
  private String name;
  private String value;
  private AzureAppServiceConnectionStringType type;

  @JsonProperty(value = "slotSetting") private boolean sticky;
}
