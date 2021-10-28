package io.harness.stateutils.buildstate;

import static io.harness.beans.sweepingoutputs.StageInfraDetails.STAGE_INFRA_DETAILS;

import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.beans.sweepingoutputs.AwsVmStageInfraDetails;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.yaml.extended.infrastrucutre.AwsVmInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.delegate.beans.ci.awsvm.CIAWSVmInitializeTaskParams;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;

import com.google.inject.Inject;

public class AWSVmInitializeTaskUtils {
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;

  public CIAWSVmInitializeTaskParams getInitializeTaskParams(
      InitializeStepInfo initializeStepInfo, Ambiance ambiance, String logPrefix) {
    Infrastructure infrastructure = initializeStepInfo.getInfrastructure();

    if (infrastructure == null || ((AwsVmInfraYaml) infrastructure).getSpec() == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }

    AwsVmInfraYaml awsVmInfraYaml = (AwsVmInfraYaml) infrastructure;
    executionSweepingOutputResolver.consume(ambiance, STAGE_INFRA_DETAILS,
        AwsVmStageInfraDetails.builder().poolId(awsVmInfraYaml.getSpec().getPoolId()).build(),
        StepOutcomeGroup.STAGE.name());

    K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(ContextElement.podDetails));

    return CIAWSVmInitializeTaskParams.builder().stageRuntimeId(k8PodDetails.getStageRuntimeID()).build();
  }
}
