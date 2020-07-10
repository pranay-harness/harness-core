package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.MANIFEST_PLAN_CREATOR;
import static io.harness.cdng.manifest.ManifestConstants.MANIFESTS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Singleton;

import io.harness.cdng.manifest.state.ManifestStep;
import io.harness.cdng.manifest.state.ManifestStepParameters;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.service.ServiceConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedList;
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
    List<ManifestConfigWrapper> stageOverrideManifests = new LinkedList<>();
    if (serviceConfig.getStageOverrides() != null) {
      stageOverrideManifests = serviceConfig.getStageOverrides().getManifests();
    }
    List<ManifestConfigWrapper> manifestOverrideSets = getManifestOverrideSetsApplicable(serviceConfig);
    return PlanNode.builder()
        .uuid(generateUuid())
        .name(MANIFESTS)
        .identifier(MANIFESTS)
        .stepType(ManifestStep.STEP_TYPE)
        .stepParameters(ManifestStepParameters.builder()
                            .serviceSpecManifests(serviceConfig.getServiceSpec().getManifests())
                            .stageOverrideManifests(stageOverrideManifests)
                            .manifestOverrideSets(manifestOverrideSets)
                            .build())
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.SYNC).build()).build())
        .build();
  }

  private List<ManifestConfigWrapper> getManifestOverrideSetsApplicable(ServiceConfig serviceConfig) {
    List<ManifestConfigWrapper> manifestOverrideSets = new LinkedList<>();
    if (serviceConfig.getStageOverrides() != null
        && serviceConfig.getStageOverrides().getUseManifestOverrideSets() != null) {
      serviceConfig.getStageOverrides()
          .getUseManifestOverrideSets()
          .stream()
          .map(useManifestOverrideSet
              -> serviceConfig.getServiceSpec()
                     .getManifestOverrideSets()
                     .stream()
                     .filter(o -> o.getIdentifier().equals(useManifestOverrideSet))
                     .findFirst())
          .forEachOrdered(optionalManifestOverrideSets -> {
            if (!optionalManifestOverrideSets.isPresent()) {
              throw new InvalidRequestException("Manifest Override Set is not defined.");
            }
            manifestOverrideSets.addAll(optionalManifestOverrideSets.get().getManifests());
          });
    }
    return manifestOverrideSets;
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
