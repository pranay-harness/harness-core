package io.harness.beans.serializer;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.product.ci.engine.proto.ParallelStep;
import io.harness.product.ci.engine.proto.Step;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ExecutionProtobufSerializer implements ProtobufSerializer<ExecutionElement> {
  @Inject private RunStepProtobufSerializer runStepProtobufSerializer;
  @Inject private PublishStepProtobufSerializer publishStepProtobufSerializer;
  @Inject private SaveCacheStepProtobufSerializer saveCacheStepProtobufSerializer;
  @Inject private RestoreCacheStepProtobufSerializer restoreCacheStepProtobufSerializer;

  @Override
  public String serialize(ExecutionElement executionElement) {
    List<Step> protoSteps = new LinkedList<>();
    if (isEmpty(executionElement.getSteps())) {
      return "";
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
                .map(this ::serialiseStep)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        protoSteps.add(
            Step.newBuilder()
                .setParallel(
                    ParallelStep.newBuilder().setId("parallel").setDisplayName("name").addAllSteps(unitStepsList))
                .build());
      }
    });

    return Base64.encodeBase64String(
        io.harness.product.ci.engine.proto.Execution.newBuilder().addAllSteps(protoSteps).build().toByteArray());
  }

  public UnitStep serialiseStep(StepElement step) {
    if (step.getStepSpecType() instanceof CIStepInfo) {
      CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
      switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
        case RUN:
          return runStepProtobufSerializer.convertRunStepInfo((RunStepInfo) ciStepInfo);
        case SAVE_CACHE:
          return saveCacheStepProtobufSerializer.convertSaveCacheStepInfo((SaveCacheStepInfo) ciStepInfo);
        case RESTORE_CACHE:
          return restoreCacheStepProtobufSerializer.convertRestoreCacheStepInfo((RestoreCacheStepInfo) ciStepInfo);
        case PUBLISH:
          return publishStepProtobufSerializer.convertRestoreCacheStepInfo((PublishStepInfo) ciStepInfo);
        case CLEANUP:
        case TEST:
        case BUILD:
        case SETUP_ENV:
        case GIT_CLONE:
        case LITE_ENGINE_TASK:
        default:
          logger.info("serialisation is not implemented");
          return null;
      }
    } else {
      throw new IllegalArgumentException("Non CISteps serialisation is not supported");
    }
  }
}
