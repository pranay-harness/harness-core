package io.harness.beans.seriazlier;

import com.google.inject.Inject;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.product.ci.engine.proto.Step;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StepElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ExecutionProtobufSerializer implements ProtobufSerializer<ExecutionElement> {
  @Inject private RunStepProtobufSerializer runStepProtobufSerializer;
  @Inject private PublishStepProtobufSerializer publishStepProtobufSerializer;
  @Inject private SaveCacheStepProtobufSerializer saveCacheStepProtobufSerializer;
  @Inject private RestoreCacheStepProtobufSerializer restoreCacheStepProtobufSerializer;

  @Override
  public String serialize(ExecutionElement object) {
    List<StepElement> steps =
        object.getSteps().stream().map(executionWrapper -> (StepElement) executionWrapper).collect(Collectors.toList());
    List<Step> protoSteps = new LinkedList<>();
    for (StepElement step : steps) {
      if (step.getStepSpecType() instanceof CIStepInfo) {
        CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
        switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
          case RUN:
            protoSteps.add(runStepProtobufSerializer.convertRunStepInfo((RunStepInfo) ciStepInfo));
            break;
          case SAVE_CACHE:
            protoSteps.add(saveCacheStepProtobufSerializer.convertSaveCacheStepInfo((SaveCacheStepInfo) ciStepInfo));
            break;
          case RESTORE_CACHE:
            protoSteps.add(
                restoreCacheStepProtobufSerializer.convertRestoreCacheStepInfo((RestoreCacheStepInfo) ciStepInfo));
            break;
          case PUBLISH:
            protoSteps.add(publishStepProtobufSerializer.convertRestoreCacheStepInfo((PublishStepInfo) ciStepInfo));
            break;
          case CLEANUP:
          case TEST:
          case BUILD:
          case SETUP_ENV:
          case GIT_CLONE:
          case LITE_ENGINE_TASK:
          default:
            logger.info("not implemented");
        }
      }
    }
    return Base64.encodeBase64String(
        io.harness.product.ci.engine.proto.Execution.newBuilder().addAllSteps(protoSteps).build().toByteArray());
  }
}
