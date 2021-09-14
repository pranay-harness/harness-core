/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SCHEDULED")
@JsonPropertyOrder({"harnessApiVersion"})
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class ScheduleTriggerConditionYaml extends TriggerConditionYaml {
  private String cronExpression;
  private String cronDescription;
  private boolean onNewArtifact;

  public ScheduleTriggerConditionYaml() {
    super.setType("SCHEDULED");
  }
  @lombok.Builder
  public ScheduleTriggerConditionYaml(String cronExpression, String cronDescription, boolean onNewArtifact) {
    super.setType("SCHEDULED");
    this.cronExpression = cronExpression;
    this.cronDescription = cronDescription;
    this.onNewArtifact = onNewArtifact;
  }
}
