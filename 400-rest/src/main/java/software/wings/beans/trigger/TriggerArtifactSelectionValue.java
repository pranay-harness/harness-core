/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(CDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "artifactSelectionType", include = EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = TriggerArtifactSelectionLastDeployed.class, name = "LAST_DEPLOYED")
  , @JsonSubTypes.Type(value = TriggerArtifactSelectionLastCollected.class, name = "LAST_COLLECTED"),
      @JsonSubTypes.Type(value = TriggerArtifactSelectionFromSource.class, name = "ARTIFACT_SOURCE"),
      @JsonSubTypes.Type(value = TriggerArtifactSelectionFromPipelineSource.class, name = "PIPELINE_SOURCE"),
      @JsonSubTypes.Type(value = TriggerArtifactSelectionWebhook.class, name = "WEBHOOK_VARIABLE")
})
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public interface TriggerArtifactSelectionValue {
  enum ArtifactSelectionType { ARTIFACT_SOURCE, LAST_COLLECTED, LAST_DEPLOYED, PIPELINE_SOURCE, WEBHOOK_VARIABLE }

  ArtifactSelectionType getArtifactSelectionType();
}
