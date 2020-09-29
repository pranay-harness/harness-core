package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.beans.CIPipelineSetupParameters;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.beans.steps.stepinfo.TestStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.beans.yaml.extended.CustomVariables;
import io.harness.beans.yaml.extended.container.Container;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.ci.stdvars.BuildStandardVariables;
import io.harness.serializer.KryoRegistrar;

/**
 * Class will register all kryo classes
 */

public class CIBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(K8PodDetails.class, 100001);
    kryo.register(ContextElement.class, 100002);
    kryo.register(BuildEnvSetupStepInfo.class, 100003);
    kryo.register(CIPipelineSetupParameters.class, 100004);
    kryo.register(CleanupStepInfo.class, 100005);
    kryo.register(GitCloneStepInfo.class, 100006);
    kryo.register(IntegrationStageStepParameters.class, 100007);
    kryo.register(LiteEngineTaskStepInfo.class, 100008);
    kryo.register(PublishStepInfo.class, 100009);
    kryo.register(RestoreCacheStepInfo.class, 100010);
    kryo.register(RunStepInfo.class, 100011);
    kryo.register(SaveCacheStepInfo.class, 100012);
    kryo.register(TestStepInfo.class, 100013);
    kryo.register(StepTaskDetails.class, 100014);
    kryo.register(BuildStandardVariables.class, 100015);
    kryo.register(CIExecutionArgs.class, 100016);
    kryo.register(BuildNumber.class, 100017);
    kryo.register(IntegrationStage.class, 100018);
    kryo.register(Container.class, 100019);
    kryo.register(Container.Resources.class, 100020);
    kryo.register(Container.Limit.class, 100021);
    kryo.register(Container.Reserve.class, 100022);
    kryo.register(CustomVariables.class, 100023);
  }
}
