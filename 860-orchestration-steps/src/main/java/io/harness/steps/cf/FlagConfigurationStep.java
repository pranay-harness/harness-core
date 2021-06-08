package io.harness.steps.cf;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.CFApi;
import io.harness.cf.openapi.model.Feature;
import io.harness.cf.openapi.model.PatchInstruction;
import io.harness.cf.openapi.model.PatchOperation;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
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
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CF)
@Slf4j
public class FlagConfigurationStep implements SyncExecutable<StepElementParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(OrchestrationStepTypes.FLAG_CONFIGURATION).build();
  private static String INFRASTRUCTURE_COMMAND_UNIT = "Execute";
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject @Named("cfPipelineAPI") private CFApi cfApi;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    log.info("Executing feature update step..");
    long startTime = System.currentTimeMillis();
    NGLogCallback ngManagerLogCallback =
        new NGLogCallback(logStreamingStepClientFactory, ambiance, INFRASTRUCTURE_COMMAND_UNIT, true);
    try {
      FlagConfigurationStepParameters flagConfigurationStepParameters =
          (FlagConfigurationStepParameters) stepParameters.getSpec();
      String featureIdentifier = flagConfigurationStepParameters.getFeature().getValue();
      ngManagerLogCallback.saveExecutionLog(format("Updating feature flag %s", featureIdentifier));

      List<PatchInstruction> instructions = new ArrayList<>();

      for (io.harness.steps.cf.PatchInstruction patchInstruction : flagConfigurationStepParameters.getInstructions()) {
        if (patchInstruction.getType().equals(Type.SET_FEATURE_FLAG_STATE)) {
          SetFeatureFlagStateYamlSpec spec = ((SetFeatureFlagStateYaml) patchInstruction).getSpec();
          PatchInstruction instruction =
              cfApi.getFeatureFlagOnPatchInstruction(spec.getState().equals("On") || spec.getState().equals("on"));
          instructions.add(instruction);
        }

        if (patchInstruction.getType().equals(Type.ADD_TARGETS_TO_VARIATION_TARGET_MAP)) {
          AddTargetsToVariationTargetMapYamlSpec spec =
              ((AddTargetsToVariationTargetMapYaml) patchInstruction).getSpec();
          PatchInstruction instruction =
              cfApi.getAddTargetToVariationMapParams(spec.getVariation().getValue(), spec.getTargets().getValue());
          instructions.add(instruction);
        }

        if (patchInstruction.getType().equals(Type.REMOVE_TARGETS_TO_VARIATION_MAP)) {
          RemoveTargetsToVariationTargetMapYamlSpec spec =
              ((RemoveTargetsToVariationTargetMapYaml) patchInstruction).getSpec();
          PatchInstruction instruction =
              cfApi.getRemoveTargetToVariationMapParams(spec.getVariation().getValue(), spec.getTargets().getValue());
          instructions.add(instruction);
        }

        if (patchInstruction.getType().equals(Type.ADD_SEGMENT_TO_VARIATION_MAP)) {
          AddSegmentToVariationTargetMapYamlSpec spec =
              ((AddSegmentToVariationTargetMapYaml) patchInstruction).getSpec();
          PatchInstruction instruction =
              cfApi.getAddSegmentToVariationMapParams(spec.getVariation().getValue(), spec.getSegments().getValue());
          instructions.add(instruction);
        }

        if (patchInstruction.getType().equals(Type.REMOVE_SEGMENT_TO_VARIATION_TARGET_MAP)) {
          RemoveSegmentToVariationTargetMapYamlSpec spec =
              ((RemoveSegmentToVariationTargetMapYaml) patchInstruction).getSpec();
          PatchInstruction instruction =
              cfApi.getAddSegmentToVariationMapParams(spec.getVariation().getValue(), spec.getSegments().getValue());
          instructions.add(instruction);
        }
      }

      PatchOperation patchOperation = PatchOperation.builder().instructions(instructions).build();

      Feature feature = cfApi.patchFeature(featureIdentifier, ambiance.getSetupAbstractionsMap().get("accountId"),
          ambiance.getSetupAbstractionsMap().get("orgIdentifier"),
          ambiance.getSetupAbstractionsMap().get("projectIdentifier"),
          flagConfigurationStepParameters.getEnvironment().getValue(), patchOperation);
    } catch (Exception e) {
      ngManagerLogCallback.saveExecutionLog(
          "Feature flag update failed", LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      return StepResponse.builder()
          .status(Status.FAILED)
          .failureInfo(FailureInfo.newBuilder().setErrorMessage(e.getMessage()).build())
          .unitProgressList(Collections.singletonList(UnitProgress.newBuilder()
                                                          .setUnitName(INFRASTRUCTURE_COMMAND_UNIT)
                                                          .setStatus(UnitStatus.FAILURE)
                                                          .setStartTime(startTime)
                                                          .setEndTime(System.currentTimeMillis())
                                                          .build()))
          .build();
    }

    ngManagerLogCallback.saveExecutionLog(
        "Feature flag update completed", LogLevel.INFO, CommandExecutionStatus.SUCCESS);

    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .unitProgressList(Collections.singletonList(UnitProgress.newBuilder()
                                                        .setUnitName(INFRASTRUCTURE_COMMAND_UNIT)
                                                        .setStatus(UnitStatus.SUCCESS)
                                                        .setStartTime(startTime)
                                                        .setEndTime(System.currentTimeMillis())
                                                        .build()))
        .build();
  }

  public StepType getType() {
    return STEP_TYPE;
  }
}
