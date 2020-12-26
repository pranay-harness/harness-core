package io.harness.beans.serializer;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.TestIntelligenceStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.TestIntelligenceStep;
import io.harness.product.ci.engine.proto.UnitStep;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;

public class TestIntelligenceStepProtobufSerializer implements ProtobufStepSerializer<TestIntelligenceStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  public UnitStep serializeStep(StepElementConfig step, Integer port, String callbackId) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    TestIntelligenceStepInfo testIntelligenceStepInfo = (TestIntelligenceStepInfo) ciStepInfo;

    long timeout = TimeoutUtils.parseTimeoutString(step.getTimeout(), ciStepInfo.getDefaultTimeout());

    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }

    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    TestIntelligenceStep.Builder testIntelligenceStepBuilder = TestIntelligenceStep.newBuilder();
    testIntelligenceStepBuilder.setGoals(testIntelligenceStepInfo.getGoals());
    testIntelligenceStepBuilder.setContainerPort(port);
    testIntelligenceStepBuilder.setLanguage(testIntelligenceStepInfo.getLanguage());
    testIntelligenceStepBuilder.setBuildTool(testIntelligenceStepInfo.getBuildTool());
    testIntelligenceStepBuilder.setContext(StepContext.newBuilder()
                                               .setNumRetries(testIntelligenceStepInfo.getRetry())
                                               .setExecutionTimeoutSecs(timeout)
                                               .build());

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setTaskId(callbackId)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(testIntelligenceStepInfo.getDisplayName()).orElse(""))
        .setTestIntelligence(testIntelligenceStepBuilder.build())
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .build();
  }
}
