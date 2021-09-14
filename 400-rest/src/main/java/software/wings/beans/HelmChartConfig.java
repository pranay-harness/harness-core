/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.validator.Trimmed;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;

@Data
@Builder
@FieldNameConstants(innerTypeName = "HelmChartConfigKeys")
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class HelmChartConfig {
  @Trimmed private String connectorId;
  @Trimmed private String chartName;
  @Trimmed private String chartVersion;
  @Trimmed private String chartUrl;
  @Transient @JsonInclude(Include.NON_EMPTY) private String connectorName;
  private String basePath;

  public String getBasePath() {
    return basePath == null ? "" : basePath;
  }
}
