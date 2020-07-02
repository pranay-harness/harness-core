package io.harness.beans.seriazlier;

import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.product.ci.engine.proto.RestoreCacheStep;
import io.harness.product.ci.engine.proto.Step;
import org.apache.commons.codec.binary.Base64;

import java.util.Optional;

public class RestoreCacheStepProtobufSerializer implements ProtobufSerializer<RestoreCacheStepInfo> {
  @Override
  public String serialize(RestoreCacheStepInfo object) {
    return Base64.encodeBase64String(convertRestoreCacheStepInfo(object).toByteArray());
  }

  private Step convertRestoreCacheStepInfo(RestoreCacheStepInfo restoreCacheStepInfo) {
    RestoreCacheStepInfo.RestoreCache restoreCache = restoreCacheStepInfo.getRestoreCache();
    RestoreCacheStep.Builder restoreCacheBuilder = RestoreCacheStep.newBuilder();
    restoreCacheBuilder.setKey(restoreCache.getKey());
    restoreCacheBuilder.setFailIfNotExist(restoreCache.isFailIfNotExist());

    return Step.newBuilder()
        .setId(restoreCacheStepInfo.getIdentifier())
        .setDisplayName(Optional.ofNullable(restoreCacheStepInfo.getDisplayName()).orElse(""))
        .setRestoreCache(restoreCacheBuilder.build())
        .build();
  }
}
