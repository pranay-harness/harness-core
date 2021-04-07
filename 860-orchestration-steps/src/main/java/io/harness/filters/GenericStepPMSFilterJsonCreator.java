package io.harness.filters;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP;
import static io.harness.yaml.core.LevelNodeQualifierName.PATH_CONNECTOR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(PIPELINE)
public abstract class GenericStepPMSFilterJsonCreator implements FilterJsonCreator<StepElementConfig> {
  public abstract Set<String> getSupportedStepTypes();

  @Override
  public Class<StepElementConfig> getFieldClass() {
    return StepElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    Set<String> stepTypes = getSupportedStepTypes();
    if (EmptyPredicate.isEmpty(stepTypes)) {
      return Collections.emptyMap();
    }
    return Collections.singletonMap(STEP, stepTypes);
  }

  @Override
  public FilterCreationResponse handleNode(FilterCreationContext filterCreationContext, StepElementConfig yamlField) {
    if (WithConnectorRef.class.isAssignableFrom(yamlField.getStepSpecType().getClass())) {
      String accountIdentifier = filterCreationContext.getSetupMetadata().getAccountId();
      String orgIdentifier = filterCreationContext.getSetupMetadata().getOrgId();
      String projectIdentifier = filterCreationContext.getSetupMetadata().getProjectId();
      List<ParameterField<String>> connectorRefs =
          ((WithConnectorRef) yamlField.getStepSpecType()).extractConnectorRefs();
      String fullQualifiedDomainName =
          YamlUtils.getFullyQualifiedName(filterCreationContext.getCurrentField().getNode()) + PATH_CONNECTOR
          + "connectorRef";
      List<EntityDetailProtoDTO> result =
          connectorRefs.stream()
              .filter(Objects::nonNull)
              .map(connectorRef
                  -> FilterCreatorHelper.convertToEntityDetailProtoDTO(
                      accountIdentifier, orgIdentifier, projectIdentifier, fullQualifiedDomainName, connectorRef))
              .collect(Collectors.toList());
      return FilterCreationResponse.builder().referredEntities(result).build();
    }
    return FilterCreationResponse.builder().build();
  }
}
