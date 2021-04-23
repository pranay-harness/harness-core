package io.harness.steps.cf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.CFApi;
import io.harness.cf.openapi.model.Feature;
import io.harness.cf.openapi.model.PatchInstruction;
import io.harness.cf.openapi.model.PatchOperation;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.steps.cf.AddSegmentToVariationTargetMapYaml.AddSegmentToVariationTargetMapYamlSpec;
import io.harness.steps.cf.AddTargetsToVariationTargetMapYaml.AddTargetsToVariationTargetMapYamlSpec;
import io.harness.steps.cf.PatchInstruction.Type;
import io.harness.steps.cf.RemoveSegmentToVariationTargetMapYaml.RemoveSegmentToVariationTargetMapYamlSpec;
import io.harness.steps.cf.RemoveTargetsToVariationTargetMapYaml.RemoveTargetsToVariationTargetMapYamlSpec;
import io.harness.steps.cf.SetFeatureFlagStateYaml.SetFeatureFlagStateYamlSpec;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CF)
@Slf4j
public class FeatureUpdateStep implements SyncExecutable<StepElementParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.FEATURE_UPDATE).build();
  @Inject @Named("cfPipelineAPI") private CFApi cfApi;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    log.info("Executing feature update step..");
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    try {
      FeatureUpdateStepParameters featureUpdateStepParameters = (FeatureUpdateStepParameters) stepParameters.getSpec();

      List<PatchInstruction> instructions = new ArrayList<>();

      for (io.harness.steps.cf.PatchInstruction patchInstruction : featureUpdateStepParameters.getInstructions()) {
        if (patchInstruction.getType().equals(Type.SET_FEATURE_FLAG_STATE)) {
          SetFeatureFlagStateYamlSpec spec = ((SetFeatureFlagStateYaml) patchInstruction).getSpec();
          PatchInstruction instruction =
              cfApi.getFeatureFlagOnPatchInstruction(spec.getState().equals("On") || spec.getState().equals("on"));
          instructions.add(instruction);
        }

        if (patchInstruction.getType().equals(Type.ADD_TARGETS_TO_VARIATION_TARGET_MAP)) {
          AddTargetsToVariationTargetMapYamlSpec spec =
              ((AddTargetsToVariationTargetMapYaml) patchInstruction).getSpec();
          PatchInstruction instruction = cfApi.getAddTargetToVariationMapParams(spec.getVariation(), spec.getTargets());
          instructions.add(instruction);
        }

        if (patchInstruction.getType().equals(Type.REMOVE_TARGETS_TO_VARIATION_MAP)) {
          RemoveTargetsToVariationTargetMapYamlSpec spec =
              ((RemoveTargetsToVariationTargetMapYaml) patchInstruction).getSpec();
          PatchInstruction instruction =
              cfApi.getRemoveTargetToVariationMapParams(spec.getVariation(), spec.getTargets());
          instructions.add(instruction);
        }

        if (patchInstruction.getType().equals(Type.ADD_SEGMENT_TO_VARIATION_MAP)) {
          AddSegmentToVariationTargetMapYamlSpec spec =
              ((AddSegmentToVariationTargetMapYaml) patchInstruction).getSpec();
          PatchInstruction instruction =
              cfApi.getAddSegmentToVariationMapParams(spec.getVariation(), spec.getSegments());
          instructions.add(instruction);
        }

        if (patchInstruction.getType().equals(Type.REMOVE_SEGMENT_TO_VARIATION_TARGET_MAP)) {
          RemoveSegmentToVariationTargetMapYamlSpec spec =
              ((RemoveSegmentToVariationTargetMapYaml) patchInstruction).getSpec();
          PatchInstruction instruction =
              cfApi.getAddSegmentToVariationMapParams(spec.getVariation(), spec.getSegments());
          instructions.add(instruction);
        }
      }

      PatchOperation patchOperation = PatchOperation.builder().instructions(instructions).build();

      Feature feature = cfApi.patchFeature(featureUpdateStepParameters.getFeature().getValue(),
          ambiance.getSetupAbstractionsMap().get("accountId"), ambiance.getSetupAbstractionsMap().get("orgIdentifier"),
          ambiance.getSetupAbstractionsMap().get("projectIdentifier"),
          featureUpdateStepParameters.getEnvironment().getValue(), patchOperation);
    } catch (Exception e) {
      stepResponseBuilder.status(Status.FAILED);
      stepResponseBuilder.failureInfo(
          FailureInfo.newBuilder().addFailureData(FailureData.newBuilder().setMessage(e.getMessage()).build()).build());
    }

    stepResponseBuilder.status(Status.SUCCEEDED);
    return stepResponseBuilder.build();
  }

  public StepType getType() {
    return STEP_TYPE;
  }
}
