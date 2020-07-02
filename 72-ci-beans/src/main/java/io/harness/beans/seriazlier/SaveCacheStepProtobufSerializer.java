package io.harness.beans.seriazlier;

import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.product.ci.engine.proto.SaveCacheStep;
import io.harness.product.ci.engine.proto.Step;
import org.apache.commons.codec.binary.Base64;

import java.util.Optional;

public class SaveCacheStepProtobufSerializer implements ProtobufSerializer<SaveCacheStepInfo> {
  @Override
  public String serialize(SaveCacheStepInfo object) {
    return Base64.encodeBase64String(convertSaveCacheStepInfo(object).toByteArray());
  }

  private Step convertSaveCacheStepInfo(SaveCacheStepInfo saveCacheStepInfo) {
    SaveCacheStepInfo.SaveCache saveCache = saveCacheStepInfo.getSaveCache();
    SaveCacheStep.Builder saveCacheBuilder = SaveCacheStep.newBuilder();
    saveCacheBuilder.addAllPaths(saveCache.getPaths());
    saveCacheBuilder.setKey(saveCache.getKey());

    return Step.newBuilder()
        .setId(saveCacheStepInfo.getIdentifier())
        .setDisplayName(Optional.ofNullable(saveCacheStepInfo.getDisplayName()).orElse(""))
        .setSaveCache(saveCacheBuilder.build())
        .build();
  }
}
