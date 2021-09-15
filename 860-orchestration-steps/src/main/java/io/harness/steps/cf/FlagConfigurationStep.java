package io.harness.steps.cf;

import static java.lang.String.format;
import static org.joda.time.Minutes.minutes;

import io.harness.OrchestrationStepConfig;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.CFApi;
import io.harness.cf.openapi.ApiException;
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
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.security.JWTTokenServiceUtils;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.steps.StepUtils;
import io.harness.steps.cf.AddRuleYaml.AddRuleYamlSpec;
import io.harness.steps.cf.AddSegmentToVariationTargetMapYaml.AddSegmentToVariationTargetMapYamlSpec;
import io.harness.steps.cf.AddTargetsToVariationTargetMapYaml.AddTargetsToVariationTargetMapYamlSpec;
import io.harness.steps.cf.PatchInstruction.Type;
import io.harness.steps.cf.RemoveSegmentToVariationTargetMapYaml.RemoveSegmentToVariationTargetMapYamlSpec;
import io.harness.steps.cf.RemoveTargetsToVariationTargetMapYaml.RemoveTargetsToVariationTargetMapYamlSpec;
import io.harness.steps.cf.SetFeatureFlagStateYaml.SetFeatureFlagStateYamlSpec;
import io.harness.steps.cf.UpdateRuleYaml.UpdateRuleYamlSpec;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CF)
@Slf4j
public class FlagConfigurationStep implements SyncExecutable<StepElementParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(OrchestrationStepTypes.FLAG_CONFIGURATION)
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  private static final String INFRASTRUCTURE_COMMAND_UNIT = "Execute";
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject @Named("cfPipelineAPI") private CFApi cfApi;
  @Inject OrchestrationStepConfig config;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public List<String> getLogKeys(Ambiance ambiance) {
    // TODO need to figure out how this should work...
    return StepUtils.generateLogKeys(ambiance, new ArrayList<>());
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    log.info("Executing feature update step..");
    long startTime = System.currentTimeMillis();
    NGLogCallback ngManagerLogCallback = new NGLogCallback(logStreamingStepClientFactory, ambiance, null, true);

    try {
      ngManagerLogCallback.saveExecutionLog("Starting Flag Update", LogLevel.INFO);

      FlagConfigurationStepParameters flagConfigurationStepParameters =
          (FlagConfigurationStepParameters) stepParameters.getSpec();
      String featureIdentifier = flagConfigurationStepParameters.getFeature().getValue();
      ngManagerLogCallback.saveExecutionLog(format("updating Feature flag %s", featureIdentifier), LogLevel.INFO);

      List<PatchInstruction> instructions = new ArrayList<>();

      for (io.harness.steps.cf.PatchInstruction patchInstruction : flagConfigurationStepParameters.getInstructions()) {
        if (patchInstruction.getType().equals(Type.SET_FEATURE_FLAG_STATE)) {
          SetFeatureFlagStateYamlSpec spec = ((SetFeatureFlagStateYaml) patchInstruction).getSpec();
          PatchInstruction instruction =
              cfApi.getFeatureFlagOnPatchInstruction(parseStateAsBoolean(spec.getState().getValue()));
          instructions.add(instruction);
          ngManagerLogCallback.saveExecutionLog(
              format("setting flag state to %s", spec.getState().getValue()), LogLevel.INFO);
        }

        if (patchInstruction.getType().equals(Type.ADD_RULE)) {
          AddRuleYamlSpec spec = ((AddRuleYaml) patchInstruction).getSpec();
          instructions.add(addRule(spec));
          ngManagerLogCallback.saveExecutionLog(format("adding rule to flag"), LogLevel.INFO);
        }

        if (patchInstruction.getType().equals(Type.UPDATE_RULE)) {
          UpdateRuleYamlSpec spec = ((UpdateRuleYaml) patchInstruction).getSpec();
          PatchInstruction instruction = cfApi.updatePercentageRollout(spec.getRuleID().getValue(), spec.getServe());
          instructions.add(instruction);
          ngManagerLogCallback.saveExecutionLog(format("updating rule for flag"), LogLevel.INFO);
        }

        if (patchInstruction.getType().equals(Type.ADD_TARGETS_TO_VARIATION_TARGET_MAP)) {
          if (patchInstruction instanceof AddTargetsToVariationTargetMapYaml) {
            AddTargetsToVariationTargetMapYamlSpec spec =
                ((AddTargetsToVariationTargetMapYaml) patchInstruction).getSpec();
            PatchInstruction instruction =
                cfApi.getAddTargetToVariationMapParams(spec.getVariation().getValue(), spec.getTargets().getValue());
            instructions.add(instruction);
          }
        }

        if (patchInstruction.getType().equals(Type.REMOVE_TARGETS_TO_VARIATION_TARGET_MAP)) {
          if (patchInstruction instanceof RemoveTargetsToVariationTargetMapYaml) {
            RemoveTargetsToVariationTargetMapYamlSpec spec =
                ((RemoveTargetsToVariationTargetMapYaml) patchInstruction).getSpec();
            PatchInstruction instruction =
                cfApi.getRemoveTargetToVariationMapParams(spec.getVariation().getValue(), spec.getTargets().getValue());
            instructions.add(instruction);
          }
        }

        if (patchInstruction.getType().equals(Type.ADD_SEGMENT_TO_VARIATION_TARGET_MAP)) {
          if (patchInstruction instanceof AddSegmentToVariationTargetMapYaml) {
            AddSegmentToVariationTargetMapYamlSpec spec =
                ((AddSegmentToVariationTargetMapYaml) patchInstruction).getSpec();
            PatchInstruction instruction =
                cfApi.getAddSegmentToVariationMapParams(spec.getVariation().getValue(), spec.getSegments().getValue());
            instructions.add(instruction);
          }
        }

        if (patchInstruction.getType().equals(Type.REMOVE_SEGMENT_TO_VARIATION_TARGET_MAP)) {
          if (patchInstruction instanceof RemoveSegmentToVariationTargetMapYaml) {
            RemoveSegmentToVariationTargetMapYamlSpec spec =
                ((RemoveSegmentToVariationTargetMapYaml) patchInstruction).getSpec();
            PatchInstruction instruction =
                cfApi.getAddSegmentToVariationMapParams(spec.getVariation().getValue(), spec.getSegments().getValue());
            instructions.add(instruction);
          }
        }
      }

      PatchOperation patchOperation = PatchOperation.builder().instructions(instructions).build();

      addApiKeyHeader(cfApi);

      cfApi.patchFeature(featureIdentifier, ambiance.getSetupAbstractionsMap().get("accountId"),
          ambiance.getSetupAbstractionsMap().get("orgIdentifier"),
          ambiance.getSetupAbstractionsMap().get("projectIdentifier"),
          flagConfigurationStepParameters.getEnvironment().getValue(), patchOperation);

      ngManagerLogCallback.saveExecutionLog(format("Update of Feature flag %s completed", featureIdentifier),
          LogLevel.INFO, CommandExecutionStatus.SUCCESS);

    } catch (ApiException e) {
      log.error(format("error updating flag because %s", e.getResponseBody()));
      return StepResponse.builder()
          .status(Status.ERRORED)
          .failureInfo(FailureInfo.newBuilder().setErrorMessage(e.getResponseBody()).build())
          .unitProgressList(Collections.singletonList(UnitProgress.newBuilder()
                                                          .setUnitName(INFRASTRUCTURE_COMMAND_UNIT)
                                                          .setStatus(UnitStatus.FAILURE)
                                                          .setStartTime(startTime)
                                                          .setEndTime(System.currentTimeMillis())
                                                          .build()))
          .build();

    } catch (Exception e) {
      log.error(format("error updating flag because %s", e.getMessage()));
      return StepResponse.builder()
          .status(Status.ERRORED)
          .failureInfo(FailureInfo.newBuilder().setErrorMessage(e.getMessage()).build())
          .unitProgressList(Collections.singletonList(UnitProgress.newBuilder()
                                                          .setUnitName(INFRASTRUCTURE_COMMAND_UNIT)
                                                          .setStatus(UnitStatus.FAILURE)
                                                          .setStartTime(startTime)
                                                          .setEndTime(System.currentTimeMillis())
                                                          .build()))
          .build();
    }

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

  private void addApiKeyHeader(CFApi cfApi) {
    String apiKey = JWTTokenServiceUtils.generateJWTToken(ImmutableMap.of("type", "APIKey", "name", "PIPELINE-SERVICE"),
        minutes(10).toStandardDuration().getMillis(), config.getFfServerApiKey());
    cfApi.getApiClient().addDefaultHeader("api-key", "Bearer " + apiKey);
    log.info("FF Server API Key: {}", apiKey);
  }

  /**
   * Converts the flag state off|on to a boolean value
   * i.e. off = false, on = true.
   * @param state indicates the state of the flag on | off
   * @return true if flag state is on
   */
  private static boolean parseStateAsBoolean(String state) {
    return (state != null) && state.equalsIgnoreCase("on");
  }

  private PatchInstruction addRule(AddRuleYamlSpec rule) {
    Integer priority = 0;
    if (ParameterField.isNull(rule.getPriority()) != true) {
      priority = rule.getPriority().getValue();
    }

    return cfApi.addPercentageRollout(rule.getUuid().getValue(), priority, rule.getServe(), rule.getClauses());
  }
}
