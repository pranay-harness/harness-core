/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("manifestsOutcome")
@JsonTypeName("manifestsOutcome")
@OwnedBy(CDC)
@RecasterAlias("io.harness.cdng.manifest.yaml.ManifestsOutcome")
public class ManifestsOutcome implements Outcome {
  @NotEmpty List<ManifestOutcome> manifestOutcomeList;
  @NotEmpty List<ManifestOutcome> manifestOriginalList;
  @NotEmpty List<ManifestOutcome> manifestStageOverridesList;
  @Singular Map<String, List<ManifestOutcome>> manifestOverrideSets;
}
