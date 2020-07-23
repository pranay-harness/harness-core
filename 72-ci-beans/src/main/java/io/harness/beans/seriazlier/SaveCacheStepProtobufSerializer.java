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

  public Step convertSaveCacheStepInfo(SaveCacheStepInfo saveCacheStepInfo) {
    SaveCacheStep.Builder saveCacheBuilder = SaveCacheStep.newBuilder();
    saveCacheBuilder.addAllPaths(saveCacheStepInfo.getPaths());
    saveCacheBuilder.setKey(saveCacheStepInfo.getKey());

    return Step.newBuilder()
        .setId(saveCacheStepInfo.getIdentifier())
        .setDisplayName(Optional.ofNullable(saveCacheStepInfo.getDisplayName()).orElse(""))
        .setSaveCache(saveCacheBuilder.build())
        .build();
  }
}
