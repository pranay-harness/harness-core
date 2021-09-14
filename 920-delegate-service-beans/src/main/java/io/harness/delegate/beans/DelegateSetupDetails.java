/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.DEL)
public class DelegateSetupDetails implements YamlDTO {
  private String sessionIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  @NotNull private String name;
  private String description;
  @NotNull private DelegateSize size;
  @NotNull private String delegateConfigurationId;

  @EntityIdentifier(allowBlank = true) private String identifier; // TODO make mandatory when UI does the change

  private K8sConfigDetails k8sConfigDetails;

  private Set<String> tags;
}
