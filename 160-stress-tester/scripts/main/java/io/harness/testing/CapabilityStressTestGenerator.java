package io.harness.testing;

import io.harness.delegate.AccountId;
import io.harness.delegate.Capability;
import io.harness.delegate.SubmitTaskRequest;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.task.shell.ShellScriptTaskParametersNG;

import com.google.protobuf.ByteString;

public class CapabilityStressTestGenerator extends StressTestGenerator {
  private final String ACCOUNT_ID = "fill_me_in";
  private ShellScriptTaskParametersNG shellScriptTaskParameters = ShellScriptTaskParametersNG.builder()
                                                                      .executeOnDelegate(true)
                                                                      .script("echo \"hello world!\"; sleep 10")
                                                                      .accountId(ACCOUNT_ID)
                                                                      .build();

  // Construct
  // 1. Tasks that were executed before - has delegates to run these tasks - 1000 at 40 qps
  // 2. Regular tasks that are coming - with no delegates to serve them - delegates not having capabilities - 1000 at 40
  // qps
  // 3. Tasks with unique capabilities -- 1000 distinct tasks, all fired off at the approximately the same time
  @Override
  public DelegateTaskStressTest makeStressTest() {
    DelegateTaskStressTest.Builder stressTestBuilder = DelegateTaskStressTest.newBuilder();
    // stage 1
    stressTestBuilder.addStage(DelegateTaskStressTestStage.newBuilder()
                                   .addTaskRequest(createTaskRequest("good_key", "good_value"))
                                   .addTaskRequest(createTaskRequest("key", "value"))
                                   .setOffset(0)
                                   .setIterations(1000)
                                   .setQps(40));

    // stage 2
    stressTestBuilder.addStage(DelegateTaskStressTestStage.newBuilder()
                                   .addTaskRequest(createTaskRequest("bad_key", "invalid"))
                                   .setOffset(30)
                                   .setIterations(1000)
                                   .setQps(40));

    // stage 3
    DelegateTaskStressTestStage.Builder stageBuilder =
        DelegateTaskStressTestStage.newBuilder().setOffset(15).setIterations(2000).setQps(1000);
    for (int i = 0; i < 1000; i++) {
      stageBuilder.addTaskRequest(createTaskRequest("random" + i, "value" + i));
    }
    stressTestBuilder.addStage(stageBuilder);

    return stressTestBuilder.build();
  }

  private SubmitTaskRequest createTaskRequest(String key, String value) {
    return SubmitTaskRequest.newBuilder()
        .setAccountId(AccountId.newBuilder().setId(ACCOUNT_ID))
        .setDetails(
            TaskDetails.newBuilder()
                .setMode(TaskMode.ASYNC)
                .setType(TaskType.newBuilder().setType("SCRIPT"))
                .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(shellScriptTaskParameters))))
        .addCapabilities(generateTestCapability(key, value))
        .build();
  }

  private Capability generateTestCapability(String key, String value) {
    return Capability.newBuilder()
        .setKryoCapability(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(
            SystemEnvCheckerCapability.builder().systemPropertyName(key).comparate(value).build())))
        .build();
  }
}
