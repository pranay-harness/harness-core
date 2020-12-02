package io.harness.beans.serializer;

import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.product.ci.engine.proto.RestoreCacheStep;
import io.harness.product.ci.engine.proto.UnitStep;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Base64;

@Singleton
public class RestoreCacheStepProtobufSerializer implements ProtobufStepSerializer<RestoreCacheStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override
  public String serializeToBase64(RestoreCacheStepInfo object) {
    return Base64.encodeBase64String(serializeStep(object).toByteArray());
  }

  public UnitStep serializeStep(RestoreCacheStepInfo restoreCacheStepInfo) {
    RestoreCacheStep.Builder restoreCacheBuilder = RestoreCacheStep.newBuilder();
    restoreCacheBuilder.setKey(restoreCacheStepInfo.getKey());
    restoreCacheBuilder.setFailIfNotExist(restoreCacheStepInfo.isFailIfNotExist());

    return UnitStep.newBuilder()
        .setId(restoreCacheStepInfo.getIdentifier())
        .setTaskId(restoreCacheStepInfo.getCallbackId())
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(restoreCacheStepInfo.getDisplayName()).orElse(""))
        .setRestoreCache(restoreCacheBuilder.build())
        .build();
  }
}
