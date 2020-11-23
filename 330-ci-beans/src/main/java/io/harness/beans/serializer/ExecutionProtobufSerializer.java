package io.harness.beans.serializer;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.beans.steps.stepinfo.TestIntelligenceStepInfo;
import io.harness.product.ci.engine.proto.Execution;
import io.harness.product.ci.engine.proto.ParallelStep;
import io.harness.product.ci.engine.proto.Step;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;

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
public class ExecutionProtobufSerializer implements ProtobufSerializer<ExecutionElement> {
  @Inject private RunStepProtobufSerializer runStepProtobufSerializer;
  @Inject private PublishStepProtobufSerializer publishStepProtobufSerializer;
  @Inject private SaveCacheStepProtobufSerializer saveCacheStepProtobufSerializer;
  @Inject private RestoreCacheStepProtobufSerializer restoreCacheStepProtobufSerializer;
  @Inject private PluginStepProtobufSerializer pluginStepProtobufSerializer;
  @Inject private TestIntelligenceStepProtobufSerializer testIntelligenceStepProtobufSerializer;

  @Override
  public String serialize(ExecutionElement object) {
    return Base64.encodeBase64String(convertExecutionElement(object).toByteArray());
  }

  public Execution convertExecutionElement(ExecutionElement executionElement) {
    List<Step> protoSteps = new LinkedList<>();
    if (isEmpty(executionElement.getSteps())) {
      return Execution.newBuilder().build();
    }

    executionElement.getSteps().forEach(executionWrapper -> {
      if (executionWrapper instanceof StepElement) {
        UnitStep serialisedStep = serialiseStep((StepElement) executionWrapper);
        if (serialisedStep != null) {
          protoSteps.add(Step.newBuilder().setUnit(serialisedStep).build());
        }
      } else if (executionWrapper instanceof ParallelStepElement) {
        ParallelStepElement parallel = (ParallelStepElement) executionWrapper;
        List<UnitStep> unitStepsList =
            parallel.getSections()
                .stream()
                .filter(executionWrapperInParallel -> executionWrapperInParallel instanceof StepElement)
                .map(executionWrapperInParallel -> (StepElement) executionWrapperInParallel)
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

  public UnitStep serialiseStep(StepElement step) {
    if (step.getStepSpecType() instanceof CIStepInfo) {
      CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
      switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
        case RUN:
          return runStepProtobufSerializer.convertRunStepInfo((RunStepInfo) ciStepInfo);
        case PLUGIN:
          return pluginStepProtobufSerializer.convertPluginStepInfo((PluginStepInfo) ciStepInfo);
        case SAVE_CACHE:
          return saveCacheStepProtobufSerializer.convertSaveCacheStepInfo((SaveCacheStepInfo) ciStepInfo);
        case RESTORE_CACHE:
          return restoreCacheStepProtobufSerializer.convertRestoreCacheStepInfo((RestoreCacheStepInfo) ciStepInfo);
        case PUBLISH:
          return publishStepProtobufSerializer.convertRestoreCacheStepInfo((PublishStepInfo) ciStepInfo);
        case TEST_INTELLIGENCE:
          return testIntelligenceStepProtobufSerializer.convertTestIntelligenceStepInfo(
              (TestIntelligenceStepInfo) ciStepInfo);
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
}
