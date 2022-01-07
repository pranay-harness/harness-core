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

import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.trigger.TriggerConditionYamlHandler;

import com.google.inject.Singleton;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class PipelineTriggerConditionHandler extends TriggerConditionYamlHandler<PipelineTriggerConditionYaml> {
  @Override
  public PipelineTriggerConditionYaml toYaml(TriggerCondition bean, String appId) {
    PipelineTriggerCondition pipelineTriggerCondition = (PipelineTriggerCondition) bean;

    return PipelineTriggerConditionYaml.builder().pipelineName(pipelineTriggerCondition.getPipelineName()).build();
  }

  @Override
  public TriggerCondition upsertFromYaml(
      ChangeContext<PipelineTriggerConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    TriggerConditionYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    PipelineTriggerConditionYaml pipelineTriggerConditionYaml = (PipelineTriggerConditionYaml) yaml;
    String pipelineName = pipelineTriggerConditionYaml.getPipelineName();

    return PipelineTriggerCondition.builder()
        .pipelineName(pipelineTriggerConditionYaml.getPipelineName())
        .pipelineId(yamlHelper.getPipelineFromName(appId, pipelineName).getUuid())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return PipelineTriggerConditionHandler.class;
  }
}
