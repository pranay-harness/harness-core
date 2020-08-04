package io.harness.beans.serializer;

import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.product.ci.engine.proto.RestoreCacheStep;
import io.harness.product.ci.engine.proto.UnitStep;
import org.apache.commons.codec.binary.Base64;

import java.util.Optional;

public class RestoreCacheStepProtobufSerializer implements ProtobufSerializer<RestoreCacheStepInfo> {
  @Override
  public String serialize(RestoreCacheStepInfo object) {
    return Base64.encodeBase64String(convertRestoreCacheStepInfo(object).toByteArray());
  }

  public UnitStep convertRestoreCacheStepInfo(RestoreCacheStepInfo restoreCacheStepInfo) {
    RestoreCacheStep.Builder restoreCacheBuilder = RestoreCacheStep.newBuilder();
    restoreCacheBuilder.setKey(restoreCacheStepInfo.getKey());
    restoreCacheBuilder.setFailIfNotExist(restoreCacheStepInfo.isFailIfNotExist());

    return UnitStep.newBuilder()
        .setId(restoreCacheStepInfo.getIdentifier())
        .setDisplayName(Optional.ofNullable(restoreCacheStepInfo.getDisplayName()).orElse(""))
        .setRestoreCache(restoreCacheBuilder.build())
        .build();
  }
}
