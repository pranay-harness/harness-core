package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.MANIFEST_PLAN_CREATOR;
import static io.harness.cdng.manifest.ManifestConstants.MANIFESTS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Singleton;

import io.harness.cdng.manifest.state.ManifestListConfig;
import io.harness.cdng.manifest.state.ManifestStep;
import io.harness.cdng.manifest.state.ManifestStepParameters;
import io.harness.cdng.service.ServiceConfig;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class ManifestStepPlanCreator implements SupportDefinedExecutorPlanCreator<ServiceConfig> {
  @Override
  public CreateExecutionPlanResponse createPlan(ServiceConfig serviceConfig, CreateExecutionPlanContext context) {
    final PlanNode manifestExecutionNode = prepareManifestStepExecutionNode(serviceConfig);

    return CreateExecutionPlanResponse.builder()
        .planNode(manifestExecutionNode)
        .startingNodeId(manifestExecutionNode.getUuid())
        .build();
  }

  private PlanNode prepareManifestStepExecutionNode(ServiceConfig serviceConfig) {
    ManifestListConfig overrideManifests = serviceConfig.getOverrides() == null
        ? ManifestListConfig.builder().build()
        : serviceConfig.getOverrides().getManifestListConfig();
    return PlanNode.builder()
        .uuid(generateUuid())
        .name(MANIFESTS)
        .identifier(MANIFESTS)
        .stepType(ManifestStep.STEP_TYPE)
        .stepParameters(ManifestStepParameters.builder()
                            .manifestServiceSpec(serviceConfig.getServiceSpec().getManifests())
                            .manifestStageOverride(overrideManifests)
                            .build())
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.SYNC).build()).build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof ServiceConfig;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(MANIFEST_PLAN_CREATOR.getName());
  }
}
