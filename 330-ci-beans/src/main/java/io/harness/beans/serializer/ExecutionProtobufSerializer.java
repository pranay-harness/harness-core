package io.harness.beans.serializer;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.yaml.YamlUtils;
import io.harness.product.ci.engine.proto.Execution;
import io.harness.product.ci.engine.proto.ParallelStep;
import io.harness.product.ci.engine.proto.Step;
import io.harness.product.ci.engine.proto.UnitStep;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ExecutionProtobufSerializer implements ProtobufSerializer<ExecutionElementConfig> {
  @Inject private RunStepProtobufSerializer runStepProtobufSerializer;
  @Inject private PublishStepProtobufSerializer publishStepProtobufSerializer;
  @Inject private SaveCacheStepProtobufSerializer saveCacheStepProtobufSerializer;
  @Inject private RestoreCacheStepProtobufSerializer restoreCacheStepProtobufSerializer;
  @Inject private PluginStepProtobufSerializer pluginStepProtobufSerializer;
  @Inject private TestIntelligenceStepProtobufSerializer testIntelligenceStepProtobufSerializer;
  @Inject private PluginCompatibleStepSerializer pluginCompatibleStepSerializer;

  public Execution convertExecutionElement(ExecutionElementConfig executionElement,
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Map<String, String> taskIds) {
    List<Step> protoSteps = new LinkedList<>();
    if (isEmpty(executionElement.getSteps())) {
      return Execution.newBuilder().build();
    }

    executionElement.getSteps().forEach(executionWrapper -> {
      if (!executionWrapper.getStep().isNull()) {
        StepElementConfig stepElementConfig = getStepElementConfig(executionWrapper);

        UnitStep serialisedStep = serialiseStep(stepElementConfig, liteEngineTaskStepInfo, taskIds);
        if (serialisedStep != null) {
          protoSteps.add(Step.newBuilder().setUnit(serialisedStep).build());
        }
      } else if (!executionWrapper.getParallel().isNull()) {
        ParallelStepElementConfig parallelStepElementConfig = getParallelStepElementConfig(executionWrapper);
        List<UnitStep> unitStepsList =
            parallelStepElementConfig.getSections()
                .stream()
                .filter(executionWrapperInParallel -> !executionWrapperInParallel.getStep().isNull())
                .map(executionWrapperInParallel -> getStepElementConfig(executionWrapper))
                .map(stepElementConfig -> serialiseStep(stepElementConfig, liteEngineTaskStepInfo, taskIds))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        protoSteps.add(
            Step.newBuilder()
                .setParallel(
                    ParallelStep.newBuilder().setId("parallel").setDisplayName("name").addAllSteps(unitStepsList))
                .build());
      }
    });

    return Execution.newBuilder().addAllSteps(protoSteps).buildPartial();
  }

  private Integer getPort(LiteEngineTaskStepInfo liteEngineTaskStepInfo, String stepIdentifier) {
    K8BuildJobEnvInfo.PodsSetupInfo podSetupInfo =
        ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getPodsSetupInfo();
    if (isEmpty(podSetupInfo.getPodSetupInfoList())) {
      return null;
    }

    Optional<PodSetupInfo> podSetupInfoOptional = podSetupInfo.getPodSetupInfoList().stream().findFirst();
    try {
      if (podSetupInfoOptional.isPresent()) {
        List<ContainerDefinitionInfo> containerDefinitionInfos =
            podSetupInfoOptional.get()
                .getPodSetupParams()
                .getContainerDefinitionInfos()
                .stream()
                .filter(containerDefinitionInfo -> {
                  return containerDefinitionInfo.getStepIdentifier().equals(stepIdentifier);
                })
                .collect(Collectors.toList());

        if (containerDefinitionInfos.size() != 1) {
          log.error("Step {} should map to single container", stepIdentifier);
          return null;
        }

        if (containerDefinitionInfos.get(0).getPorts().size() != 1) {
          log.error("Step {} should map to single port", stepIdentifier);
          return null;
        }

        return containerDefinitionInfos.get(0).getPorts().get(0);
      } else {
        return null;
      }
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to retrieve port for step " + stepIdentifier, ex);
    }
  }

  public UnitStep serialiseStep(
      StepElementConfig step, LiteEngineTaskStepInfo liteEngineTaskStepInfo, Map<String, String> taskIds) {
    if (step.getStepSpecType() instanceof CIStepInfo) {
      CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
      switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
        case RUN:
          return runStepProtobufSerializer.serializeStep(
              step, getPort(liteEngineTaskStepInfo, step.getIdentifier()), taskIds.get(step.getIdentifier()));
        case PLUGIN:
          return pluginStepProtobufSerializer.serializeStep(
              step, getPort(liteEngineTaskStepInfo, step.getIdentifier()), taskIds.get(step.getIdentifier()));
        case SAVE_CACHE:
          return saveCacheStepProtobufSerializer.serializeStep(step, null, taskIds.get(step.getIdentifier()));
        case RESTORE_CACHE:
          return restoreCacheStepProtobufSerializer.serializeStep(step, null, taskIds.get(step.getIdentifier()));
        case PUBLISH:
          return publishStepProtobufSerializer.serializeStep(step, null, taskIds.get(step.getIdentifier()));
        case GCR:
        case DOCKER:
        case ECR:
        case UPLOAD_GCS:
        case UPLOAD_S3:
        case SAVE_CACHE_GCS:
        case RESTORE_CACHE_GCS:
        case SAVE_CACHE_S3:
        case RESTORE_CACHE_S3:
          return pluginCompatibleStepSerializer.serializeStep(
              step, getPort(liteEngineTaskStepInfo, step.getIdentifier()), taskIds.get(step.getIdentifier()));
        case TEST_INTELLIGENCE:
          return testIntelligenceStepProtobufSerializer.serializeStep(
              step, getPort(liteEngineTaskStepInfo, step.getIdentifier()), taskIds.get(step.getIdentifier()));
        case CLEANUP:
        case TEST:
        case BUILD:
        case SETUP_ENV:
        case GIT_CLONE:
        case LITE_ENGINE_TASK:
        default:
          log.info("serialisation is not implemented");
          return null;
      }
    } else {
      throw new IllegalArgumentException("Non CISteps serialisation is not supported");
    }
  }

  private ParallelStepElementConfig getParallelStepElementConfig(ExecutionWrapperConfig executionWrapperConfig) {
    try {
      return YamlUtils.read(executionWrapperConfig.getParallel().toString(), ParallelStepElementConfig.class);
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to deserialize ExecutionWrapperConfig parallel node", ex);
    }
  }

  private StepElementConfig getStepElementConfig(ExecutionWrapperConfig executionWrapperConfig) {
    try {
      return YamlUtils.read(executionWrapperConfig.getStep().toString(), StepElementConfig.class);
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to deserialize ExecutionWrapperConfig step node", ex);
    }
  }
}
