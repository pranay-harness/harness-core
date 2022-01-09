/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.trigger.TriggerArtifactSelectionValue.ArtifactSelectionType;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("LAST_DEPLOYED")
@JsonPropertyOrder({"harnessApiVersion"})
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class TriggerArtifactSelectionLastDeployedYaml extends TriggerArtifactSelectionValueYaml {
  private String name;
  private String type;

  public TriggerArtifactSelectionLastDeployedYaml() {
    super.setType(ArtifactSelectionType.LAST_DEPLOYED.name());
  }

  @Builder
  public TriggerArtifactSelectionLastDeployedYaml(String name, String type) {
    super.setType(ArtifactSelectionType.LAST_DEPLOYED.name());
    this.name = name;
    this.type = type;
  }
}
