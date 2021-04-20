package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateSetupDetails {
  private String sessionIdentifier;
  @NotNull private String name;
  private String description;
  @NotNull private DelegateSize size;
  @NotNull private String delegateConfigurationId;

  private K8sConfigDetails k8sConfigDetails;
}
