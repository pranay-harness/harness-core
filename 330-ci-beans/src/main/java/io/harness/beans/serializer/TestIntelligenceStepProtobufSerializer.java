package io.harness.beans.serializer;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.TestIntelligenceStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.TestIntelligenceStep;
import io.harness.product.ci.engine.proto.UnitStep;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Base64;

public class TestIntelligenceStepProtobufSerializer implements ProtobufStepSerializer<TestIntelligenceStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override
  public String serializeToBase64(StepElementConfig object) {
    return Base64.encodeBase64String(serializeStep(object).toByteArray());
  }

  public UnitStep serializeStep(StepElementConfig step) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    TestIntelligenceStepInfo testIntelligenceStepInfo = (TestIntelligenceStepInfo) ciStepInfo;

    TestIntelligenceStep.Builder testIntelligenceStepBuilder = TestIntelligenceStep.newBuilder();
    testIntelligenceStepBuilder.setGoals(testIntelligenceStepInfo.getGoals());
    testIntelligenceStepBuilder.setContainerPort(testIntelligenceStepInfo.getPort());
    testIntelligenceStepBuilder.setLanguage(testIntelligenceStepInfo.getLanguage());
    testIntelligenceStepBuilder.setBuildTool(testIntelligenceStepInfo.getBuildTool());
    testIntelligenceStepBuilder.setContext(StepContext.newBuilder()
                                               .setNumRetries(testIntelligenceStepInfo.getRetry())
                                               .setExecutionTimeoutSecs(testIntelligenceStepInfo.getTimeout())
                                               .build());

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(testIntelligenceStepInfo.getIdentifier())
        .setTaskId(testIntelligenceStepInfo.getCallbackId())
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(testIntelligenceStepInfo.getDisplayName()).orElse(""))
        .setTestIntelligence(testIntelligenceStepBuilder.build())
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .build();
  }
}
