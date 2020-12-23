package io.harness.beans.serializer;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.product.ci.engine.proto.SaveCacheStep;
import io.harness.product.ci.engine.proto.UnitStep;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Base64;

@Singleton
public class SaveCacheStepProtobufSerializer implements ProtobufStepSerializer<SaveCacheStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override
  public String serializeToBase64(StepElementConfig object) {
    return Base64.encodeBase64String(serializeStep(object).toByteArray());
  }

  public UnitStep serializeStep(StepElementConfig step) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    SaveCacheStepInfo saveCacheStepInfo = (SaveCacheStepInfo) ciStepInfo;

    SaveCacheStep.Builder saveCacheBuilder = SaveCacheStep.newBuilder();
    saveCacheBuilder.addAllPaths(saveCacheStepInfo.getPaths());
    saveCacheBuilder.setKey(saveCacheStepInfo.getKey());

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setDisplayName(Optional.ofNullable(step.getName()).orElse(""))
        .setTaskId(saveCacheStepInfo.getCallbackId())
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setSaveCache(saveCacheBuilder.build())
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .build();
  }
}
