package io.harness.ci.serializer.vm;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.RunTestsStepInfo;
import io.harness.delegate.beans.ci.vm.steps.VmStepInfo;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.timeout.Timeout;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class VmStepSerializer {
  @Inject private VmRunStepSerializer vmRunStepSerializer;
  public VmStepInfo serialize(
      CIStepInfo stepInfo, String identifier, ParameterField<Timeout> parameterFieldTimeout) {
      String stepName = stepInfo.getNonYamlInfo().getStepInfoType().getDisplayName();
    switch (stepInfo.getNonYamlInfo().getStepInfoType()) {
      case RUN:
        return vmRunStepSerializer.serialize((RunStepInfo) stepInfo, identifier, parameterFieldTimeout, stepName);
        //            case PLUGIN:
        //                return pluginStepProtobufSerializer.serializeStepWithStepParameters((PluginStepInfo)
        //                ciStepInfo, port, taskId,
        //                        logKey, stepIdentifier, ParameterField.createValueField(Timeout.fromString(timeout)),
        //                        accountId, stepName);
        //            case GCR:
        //            case DOCKER:
        //            case ECR:
        //            case UPLOAD_ARTIFACTORY:
        //            case UPLOAD_GCS:
        //            case UPLOAD_S3:
        //            case SAVE_CACHE_GCS:
        //            case RESTORE_CACHE_GCS:
        //            case SAVE_CACHE_S3:
        //            case RESTORE_CACHE_S3:
        //                return pluginCompatibleStepSerializer.serializeStepWithStepParameters((PluginCompatibleStep)
        //                ciStepInfo, port,
        //                        taskId, logKey, stepIdentifier,
        //                        ParameterField.createValueField(Timeout.fromString(timeout)), accountId, stepName);
        //            case RUN_TESTS:
        //                return runTestsStepProtobufSerializer.serializeStepWithStepParameters((RunTestsStepInfo)
        //                ciStepInfo, port,
        //                        taskId, logKey, stepIdentifier,
        //                        ParameterField.createValueField(Timeout.fromString(timeout)), accountId, stepName);
      case CLEANUP:
      case TEST:
      case BUILD:
      case SETUP_ENV:
      case GIT_CLONE:
      case INITIALIZE_TASK:
      default:
        //                log.info("serialisation is not implemented");
        return null;
    }
  }
}
