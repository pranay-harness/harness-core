package io.harness.cdng.creator.filters;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceYaml;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.cdng.sample.cd.creator.filters.CdFilter;
import io.harness.pms.cdng.sample.cd.creator.filters.CdFilter.CdFilterBuilder;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.filter.creation.FilterCreationResponse.FilterCreationResponseBuilder;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractorVisitor;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DeploymentStageFilterJsonCreator implements FilterJsonCreator<StageElementConfig> {
  @Inject private SimpleVisitorFactory simpleVisitorFactory;

  @Override
  public Class<StageElementConfig> getFieldClass() {
    return StageElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stage", Collections.singleton("Deployment"));
  }

  @Override
  public FilterCreationResponse handleNode(FilterCreationContext filterCreationContext, StageElementConfig yamlField) {
    FilterCreationResponseBuilder creationResponse = FilterCreationResponse.builder();

    CdFilterBuilder cdFilter = CdFilter.builder();
    DeploymentStageConfig deploymentStageConfig = (DeploymentStageConfig) yamlField.getStageType();
    Set<EntityDetailProtoDTO> referredEntities = getReferences(filterCreationContext.getSetupMetadata().getAccountId(),
        filterCreationContext.getSetupMetadata().getOrgId(), filterCreationContext.getSetupMetadata().getProjectId(),
        deploymentStageConfig);
    creationResponse.referredEntities(new ArrayList<>(referredEntities));

    if (deploymentStageConfig.getExecution() == null) {
      return creationResponse.build();
    }

    ServiceYaml service = deploymentStageConfig.getServiceConfig().getService();
    if (service != null && isNotEmpty(service.getName())) {
      cdFilter.serviceName(service.getName());
    }

    ServiceDefinition serviceDefinition = deploymentStageConfig.getServiceConfig().getServiceDefinition();
    if (serviceDefinition != null && serviceDefinition.getType() != null) {
      cdFilter.deploymentType(serviceDefinition.getType());
    }

    PipelineInfrastructure infrastructure = deploymentStageConfig.getInfrastructure();
    if (infrastructure != null && infrastructure.getEnvironment() != null
        && isNotEmpty(infrastructure.getEnvironment().getName())) {
      cdFilter.environmentName(infrastructure.getEnvironment().getName());
    }

    creationResponse.pipelineFilter(cdFilter.build());
    return creationResponse.build();
  }

  private Set<EntityDetailProtoDTO> getReferences(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, DeploymentStageConfig deploymentStageConfig) {
    EntityReferenceExtractorVisitor visitor =
        simpleVisitorFactory.obtainEntityReferenceExtractorVisitor(accountIdentifier, orgIdentifier, projectIdentifier);
    visitor.walkElementTree(deploymentStageConfig);
    return visitor.getEntityReferenceSet();
  }
}
