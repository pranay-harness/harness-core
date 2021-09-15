/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.EntityType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "ArtifactSelectionKeys")
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class TriggerArtifactVariable {
  @NotEmpty private String variableName;

  @NotEmpty private String entityId;
  @NotEmpty private EntityType entityType;
  private transient String entityName;

  private TriggerArtifactSelectionValue variableValue;
}
