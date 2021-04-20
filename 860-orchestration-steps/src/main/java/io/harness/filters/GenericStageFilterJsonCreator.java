package io.harness.filters;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.SecretRefData;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.stages.stage.StageInfoConfig;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.filter.creation.FilterCreationResponse.FilterCreationResponseBuilder;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractorVisitor;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;

@OwnedBy(PIPELINE)
public abstract class GenericStageFilterJsonCreator implements FilterJsonCreator<StageElementConfig> {
  @Inject private SimpleVisitorFactory simpleVisitorFactory;

  public abstract Set<String> getSupportedStageTypes();

  public abstract PipelineFilter getFilter(
      FilterCreationContext filterCreationContext, StageElementConfig stageElementConfig);

  @Override
  public Class<StageElementConfig> getFieldClass() {
    return StageElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    Set<String> stageTypes = getSupportedStageTypes();
    if (EmptyPredicate.isEmpty(stageTypes)) {
      return Collections.emptyMap();
    }
    return Collections.singletonMap(YAMLFieldNameConstants.STAGE, stageTypes);
  }

  @Override
  public FilterCreationResponse handleNode(
      FilterCreationContext filterCreationContext, StageElementConfig stageElementConfig) {
    FilterCreationResponseBuilder creationResponse = FilterCreationResponse.builder();

    YamlField variablesField =
        filterCreationContext.getCurrentField().getNode().getField(YAMLFieldNameConstants.VARIABLES);
    if (variablesField != null) {
      FilterCreatorHelper.checkIfVariableNamesAreValid(variablesField);
    }

    creationResponse.referredEntities(new ArrayList<>(getReferredEntities(filterCreationContext, stageElementConfig)));

    if (stageElementConfig.getStageType() == null || stageElementConfig.getStageType().getExecution() == null) {
      throw new InvalidRequestException(
          String.format("Execution section is required in %s stage", stageElementConfig.getType()));
    }
    creationResponse.dependencies(getDependencies(filterCreationContext.getCurrentField()));

    PipelineFilter filter = getFilter(filterCreationContext, stageElementConfig);
    if (filter != null) {
      creationResponse.pipelineFilter(filter);
    }
    return creationResponse.build();
  }

  private Set<EntityDetailProtoDTO> getReferredEntities(
      FilterCreationContext filterCreationContext, StageElementConfig stageElementConfig) {
    Set<EntityDetailProtoDTO> referredEntities = getReferences(filterCreationContext.getSetupMetadata().getAccountId(),
        filterCreationContext.getSetupMetadata().getOrgId(), filterCreationContext.getSetupMetadata().getProjectId(),
        stageElementConfig.getStageType(), stageElementConfig.getIdentifier());
    referredEntities.addAll(extractSecretRefs(filterCreationContext));
    return referredEntities;
  }

  private Set<EntityDetailProtoDTO> getReferences(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, StageInfoConfig stageInfoConfig, String stageIdentifier) {
    List<LevelNode> levelNodes = new LinkedList<>();
    levelNodes.add(LevelNode.builder().qualifierName(YAMLFieldNameConstants.PIPELINE).build());
    levelNodes.add(LevelNode.builder().qualifierName(YAMLFieldNameConstants.STAGES).build());
    levelNodes.add(LevelNode.builder().qualifierName(stageIdentifier).build());
    EntityReferenceExtractorVisitor visitor = simpleVisitorFactory.obtainEntityReferenceExtractorVisitor(
        accountIdentifier, orgIdentifier, projectIdentifier, levelNodes);
    visitor.walkElementTree(stageInfoConfig);
    return visitor.getEntityReferenceSet();
  }

  private Set<EntityDetailProtoDTO> extractSecretRefs(FilterCreationContext context) {
    String accountId = context.getSetupMetadata().getAccountId();
    String orgId = context.getSetupMetadata().getOrgId();
    String projectId = context.getSetupMetadata().getProjectId();
    Set<EntityDetailProtoDTO> entityDetailProtoDTOS = new HashSet<>();
    YamlField variablesField = context.getCurrentField().getNode().getField(YAMLFieldNameConstants.VARIABLES);
    if (variablesField == null) {
      return entityDetailProtoDTOS;
    }
    Map<String, ParameterField<SecretRefData>> fqnToSecretRefs =
        SecretRefExtractorHelper.extractSecretRefsFromVariables(variablesField);
    for (Map.Entry<String, ParameterField<SecretRefData>> entry : fqnToSecretRefs.entrySet()) {
      entityDetailProtoDTOS.add(FilterCreatorHelper.convertSecretToEntityDetailProtoDTO(
          accountId, orgId, projectId, entry.getKey(), entry.getValue()));
    }
    return entityDetailProtoDTOS;
  }

  @NotNull
  protected Map<String, YamlField> getDependencies(YamlField stageField) {
    // Add dependency for execution
    YamlField executionField =
        stageField.getNode().getField(YAMLFieldNameConstants.SPEC).getNode().getField(YAMLFieldNameConstants.EXECUTION);
    Map<String, YamlField> dependencies = new HashMap<>();
    dependencies.put(executionField.getNode().getUuid(), executionField);
    return dependencies;
  }
}
