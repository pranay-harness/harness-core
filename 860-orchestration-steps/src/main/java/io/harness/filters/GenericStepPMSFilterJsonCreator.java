/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.filters;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP;
import static io.harness.walktree.visitor.utilities.VisitorParentPathUtils.PATH_CONNECTOR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(PIPELINE)
@TargetModule(HarnessModule._882_PMS_SDK_CORE)
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
      Map<String, ParameterField<String>> connectorRefs =
          ((WithConnectorRef) yamlField.getStepSpecType()).extractConnectorRefs();
      List<EntityDetailProtoDTO> result = new ArrayList<>();
      for (Map.Entry<String, ParameterField<String>> entry : connectorRefs.entrySet()) {
        String fullQualifiedDomainName =
            YamlUtils.getFullyQualifiedName(filterCreationContext.getCurrentField().getNode()) + PATH_CONNECTOR
            + YAMLFieldNameConstants.SPEC + PATH_CONNECTOR + entry.getKey();
        result.add(FilterCreatorHelper.convertToEntityDetailProtoDTO(accountIdentifier, orgIdentifier,
            projectIdentifier, fullQualifiedDomainName, entry.getValue(), EntityTypeProtoEnum.CONNECTORS));
      }
      return FilterCreationResponse.builder().referredEntities(result).build();
    }
    return FilterCreationResponse.builder().build();
  }
}
