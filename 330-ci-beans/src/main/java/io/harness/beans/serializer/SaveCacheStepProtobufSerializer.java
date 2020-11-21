package io.harness.beans.serializer;

import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.product.ci.engine.proto.SaveCacheStep;
import io.harness.product.ci.engine.proto.UnitStep;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Base64;

@Singleton
public class SaveCacheStepProtobufSerializer implements ProtobufSerializer<SaveCacheStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override
  public String serialize(SaveCacheStepInfo object) {
    return Base64.encodeBase64String(convertSaveCacheStepInfo(object).toByteArray());
  }

  public UnitStep convertSaveCacheStepInfo(SaveCacheStepInfo saveCacheStepInfo) {
    SaveCacheStep.Builder saveCacheBuilder = SaveCacheStep.newBuilder();
    saveCacheBuilder.addAllPaths(saveCacheStepInfo.getPaths());
    saveCacheBuilder.setKey(saveCacheStepInfo.getKey());
    return UnitStep.newBuilder()
        .setId(saveCacheStepInfo.getIdentifier())
        .setDisplayName(Optional.ofNullable(saveCacheStepInfo.getDisplayName()).orElse(""))
        .setTaskId(saveCacheStepInfo.getCallbackId())
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setSaveCache(saveCacheBuilder.build())
        .build();
  }
}
