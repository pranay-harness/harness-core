package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.ParameterFieldHelper;
import io.harness.data.structure.CollectionUtils;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.common.StageElementParameters.StageElementParametersBuilder;
import io.harness.pms.tags.TagUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.utils.NGVariablesUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class StepParametersUtils {
  public StageElementParametersBuilder getStageParameters(StageElementConfig stageElementConfig) {
    TagUtils.removeUuidFromTags(stageElementConfig.getTags());

    StageElementParametersBuilder stageBuilder = StageElementParameters.builder();
    stageBuilder.name(stageElementConfig.getName());
    stageBuilder.identifier(stageElementConfig.getIdentifier());
    stageBuilder.description(
        ParameterFieldHelper.getParameterFieldHandleValueNull(stageElementConfig.getDescription()));
    stageBuilder.failureStrategies(stageElementConfig.getFailureStrategies());
    stageBuilder.skipCondition(stageElementConfig.getSkipCondition());
    stageBuilder.when(stageElementConfig.getWhen());
    stageBuilder.type(stageElementConfig.getType());
    stageBuilder.uuid(stageElementConfig.getUuid());
    stageBuilder.variables(
        ParameterField.createValueField(NGVariablesUtils.getMapOfVariables(stageElementConfig.getVariables())));
    stageBuilder.tags(CollectionUtils.emptyIfNull(stageElementConfig.getTags()));

    return stageBuilder;
  }
}
