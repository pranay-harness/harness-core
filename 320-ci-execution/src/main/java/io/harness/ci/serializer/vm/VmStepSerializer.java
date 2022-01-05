package io.harness.ci.serializer.vm;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.RunTestsStepInfo;
import io.harness.delegate.beans.ci.vm.steps.VmStepInfo;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.util.CIVmSecretEvaluator;
import io.harness.yaml.core.timeout.Timeout;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;

@Singleton
public class VmStepSerializer {
  @Inject VmPluginCompatibleStepSerializer vmPluginCompatibleStepSerializer;
  @Inject VmPluginStepSerializer vmPluginStepSerializer;
  @Inject VmRunStepSerializer vmRunStepSerializer;
  @Inject VmRunTestStepSerializer vmRunTestStepSerializer;

  public Set<String> getStepSecrets(VmStepInfo vmStepInfo, Ambiance ambiance) {
    CIVmSecretEvaluator ciVmSecretEvaluator = CIVmSecretEvaluator.builder().build();
    return ciVmSecretEvaluator.resolve(
        vmStepInfo, AmbianceUtils.getNgAccess(ambiance), ambiance.getExpressionFunctorToken());
  }

  public VmStepInfo serialize(
      Ambiance ambiance, CIStepInfo stepInfo, String identifier, ParameterField<Timeout> parameterFieldTimeout) {
    String stepName = stepInfo.getNonYamlInfo().getStepInfoType().getDisplayName();
    switch (stepInfo.getNonYamlInfo().getStepInfoType()) {
      case RUN:
        return vmRunStepSerializer.serialize(
            (RunStepInfo) stepInfo, ambiance, identifier, parameterFieldTimeout, stepName);
      case RUN_TESTS:
        return vmRunTestStepSerializer.serialize(
            (RunTestsStepInfo) stepInfo, identifier, parameterFieldTimeout, stepName, ambiance);
      case PLUGIN:
        return vmPluginStepSerializer.serialize(
            (PluginStepInfo) stepInfo, identifier, parameterFieldTimeout, stepName, ambiance);
      case GCR:
      case DOCKER:
      case ECR:
      case UPLOAD_ARTIFACTORY:
      case UPLOAD_GCS:
      case UPLOAD_S3:
      case SAVE_CACHE_GCS:
      case RESTORE_CACHE_GCS:
      case SAVE_CACHE_S3:
      case RESTORE_CACHE_S3:
        return vmPluginCompatibleStepSerializer.serialize(
            ambiance, (PluginCompatibleStep) stepInfo, identifier, parameterFieldTimeout, stepName);
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
