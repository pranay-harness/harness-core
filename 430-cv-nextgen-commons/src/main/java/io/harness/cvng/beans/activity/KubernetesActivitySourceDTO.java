/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.activity;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeName("KUBERNETES")
@OwnedBy(HarnessTeam.CV)
public class KubernetesActivitySourceDTO extends ActivitySourceDTO {
  @NotNull String connectorIdentifier;
  @NotNull @NotEmpty Set<KubernetesActivitySourceConfig> activitySourceConfigs;

  @Override
  public ActivitySourceType getType() {
    return ActivitySourceType.KUBERNETES;
  }

  @Override
  public boolean isEditable() {
    return true;
  }

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "KubernetesActivitySourceConfigKeys")
  public static class KubernetesActivitySourceConfig {
    @NotNull String serviceIdentifier;
    @NotNull String envIdentifier;
    @NotNull String namespace;
    @NotNull String workloadName;
    String namespaceRegex;
  }
}
