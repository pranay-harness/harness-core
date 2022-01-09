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

import software.wings.yaml.BaseYamlWithType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
@JsonSubTypes({
  @Type(value = ScheduleTriggerConditionYaml.class, name = "SCHEDULED")
  , @Type(value = ArtifactTriggerConditionYaml.class, name = "NEW_ARTIFACT"),
      @Type(value = PipelineTriggerConditionYaml.class, name = "PIPELINE_COMPLETION"),
      @Type(value = WebhookEventTriggerConditionYaml.class, name = "WEBHOOK"),
      @Type(value = ManifestTriggerConditionYaml.class, name = "NEW_MANIFEST")
})
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public abstract class TriggerConditionYaml extends BaseYamlWithType {
  public TriggerConditionYaml(String type) {
    super(type);
  }
}
