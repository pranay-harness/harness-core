package io.harness.beans.serializer;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.steps.CIStepInfo;
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
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

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

  @Override
  public String serialize(ExecutionElementConfig object) {
    return Base64.encodeBase64String(convertExecutionElement(object).toByteArray());
  }

  public Execution convertExecutionElement(ExecutionElementConfig executionElement) {
    List<Step> protoSteps = new LinkedList<>();
    if (isEmpty(executionElement.getSteps())) {
      return Execution.newBuilder().build();
    }

    executionElement.getSteps().forEach(executionWrapper -> {
      if (!executionWrapper.getStep().isNull()) {
        StepElementConfig stepElementConfig = getStepElementConfig(executionWrapper);

        UnitStep serialisedStep = serialiseStep(stepElementConfig);
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
                .map(this::serialiseStep)
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

  public UnitStep serialiseStep(StepElementConfig step) {
    if (step.getStepSpecType() instanceof CIStepInfo) {
      CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
      switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
        case RUN:
          return runStepProtobufSerializer.serializeStep(step);
        case PLUGIN:
          return pluginStepProtobufSerializer.serializeStep(step);
        case SAVE_CACHE:
          return saveCacheStepProtobufSerializer.serializeStep(step);
        case RESTORE_CACHE:
          return restoreCacheStepProtobufSerializer.serializeStep(step);
        case PUBLISH:
          return publishStepProtobufSerializer.serializeStep(step);
        case GCR:
        case DOCKER:
        case ECR:
        case UPLOAD_GCS:
        case UPLOAD_S3:
        case SAVE_CACHE_GCS:
        case RESTORE_CACHE_GCS:
        case SAVE_CACHE_S3:
        case RESTORE_CACHE_S3:
          return pluginCompatibleStepSerializer.serializeStep(step);
        case TEST_INTELLIGENCE:
          return testIntelligenceStepProtobufSerializer.serializeStep(step);
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
