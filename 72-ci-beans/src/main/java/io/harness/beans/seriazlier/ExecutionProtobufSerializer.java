package io.harness.beans.seriazlier;

import com.google.inject.Inject;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.product.ci.engine.proto.Step;
import io.harness.yaml.core.Execution;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.util.LinkedList;
import java.util.List;

@Slf4j
public class ExecutionProtobufSerializer implements ProtobufSerializer<Execution> {
  @Inject private RunStepProtobufSerializer runStepProtobufSerializer;
  @Inject private PublishStepProtobufSerializer publishStepProtobufSerializer;
  @Inject private SaveCacheStepProtobufSerializer saveCacheStepProtobufSerializer;
  @Inject private RestoreCacheStepProtobufSerializer restoreCacheStepProtobufSerializer;

  @Override
  public String serialize(Execution object) {
    List<ExecutionSection> steps = object.getSteps();
    List<Step> protoSteps = new LinkedList<>();
    for (ExecutionSection step : steps) {
      if (step instanceof CIStepInfo) {
        CIStepInfo ciStepInfo = (CIStepInfo) step;

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
