package io.harness.filters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.SecretRefData;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.pipeline.filters.ChildrenFilterJsonCreator;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineFilterJsonCreator extends ChildrenFilterJsonCreator<PipelineInfoConfig> {
  @Override
  public Class<PipelineInfoConfig> getFieldClass() {
    return PipelineInfoConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("pipeline", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public Map<String, YamlField> getDependencies(FilterCreationContext filterCreationContext) {
    YamlField stagesYamlField =
        filterCreationContext.getCurrentField().getNode().getField(YAMLFieldNameConstants.STAGES);
    if (stagesYamlField == null) {
      throw new InvalidRequestException("Pipeline without stages cannot be saved");
    }
    YamlField variablesField =
        filterCreationContext.getCurrentField().getNode().getField(YAMLFieldNameConstants.VARIABLES);
    if (variablesField != null) {
      FilterCreatorHelper.checkIfVariableNamesAreValid(variablesField);
    }
    return StagesFilterJsonCreator.getDependencies(stagesYamlField);
  }

  public List<String> getStageNames(FilterCreationContext filterCreationContext, Collection<YamlField> children) {
    List<String> stageNames = new ArrayList<>();
    for (YamlField stage : children) {
      if (stage.getName().equals(YAMLFieldNameConstants.PARALLEL)) {
        stageNames.addAll(Optional.of(stage.getNode().asArray())
                              .orElse(Collections.emptyList())
                              .stream()
                              .map(el -> el.getField(YAMLFieldNameConstants.STAGE))
                              .filter(Objects::nonNull)
                              .map(YamlField::getNode)
                              .map(YamlNode::getName)
                              .collect(Collectors.toList()));
      } else if (stage.getName().equals(YAMLFieldNameConstants.STAGE)
          && EmptyPredicate.isNotEmpty(stage.getNode().getName())) {
        stageNames.add(stage.getNode().getName());
      }
    }
    return stageNames;
  }

  @Override
  public PipelineFilter getFilterForGivenField() {
    return null;
  }

  @Override
  public int getStageCount(FilterCreationContext filterCreationContext, Collection<YamlField> children) {
    return StagesFilterJsonCreator.getStagesCount(children);
  }

  @Override
  public List<EntityDetailProtoDTO> getReferredEntities(FilterCreationContext context, PipelineInfoConfig field) {
    String accountId = context.getSetupMetadata().getAccountId();
    String orgId = context.getSetupMetadata().getOrgId();
    String projectId = context.getSetupMetadata().getProjectId();
    List<EntityDetailProtoDTO> entityDetailProtoDTOS = new ArrayList<>();
    YamlField variablesField = context.getCurrentField().getNode().getField(YAMLFieldNameConstants.VARIABLES);
    if (variablesField == null) {
      return new ArrayList<>();
    }
    Map<String, ParameterField<SecretRefData>> fqnToSecretRefs =
        SecretRefExtractorHelper.extractSecretRefsFromVariables(variablesField);
    for (Map.Entry<String, ParameterField<SecretRefData>> entry : fqnToSecretRefs.entrySet()) {
      entityDetailProtoDTOS.add(FilterCreatorHelper.convertSecretToEntityDetailProtoDTO(
          accountId, orgId, projectId, entry.getKey(), entry.getValue()));
    }
    return entityDetailProtoDTOS;
  }
}
