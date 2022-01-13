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
@JsonTypeName("WEBHOOK_VARIABLE")
@JsonPropertyOrder({"harnessApiVersion"})
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class TriggerArtifactSelectionWebhookYaml extends TriggerArtifactSelectionValueYaml {
  private String artifactStreamName;
  private String artifactServerName;
  private String artifactStreamType;
  private String buildNumber;

  public TriggerArtifactSelectionWebhookYaml() {
    super.setType(ArtifactSelectionType.WEBHOOK_VARIABLE.name());
  }

  @Builder
  public TriggerArtifactSelectionWebhookYaml(
      String artifactStreamName, String buildNumber, String artifactStreamType, String artifactServerName) {
    super.setType(ArtifactSelectionType.WEBHOOK_VARIABLE.name());
    this.artifactStreamName = artifactStreamName;
    this.buildNumber = buildNumber;
    this.artifactStreamType = artifactStreamType;
    this.artifactServerName = artifactServerName;
  }
}
