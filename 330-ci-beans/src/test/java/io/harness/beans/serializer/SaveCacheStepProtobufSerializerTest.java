package io.harness.beans.serializer;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.category.element.UnitTests;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SaveCacheStepProtobufSerializerTest extends CIBeansTest {
  public static final String SAVE_CACHE = "save-cache";
  public static final String SAVE_CACHE_ID = "save-cache-id";
  public static final String SAVE_CACHE_KEY = "save-cache-key";
  public static final String CALLBACK_ID = "callbackId";
  public static final String PATH_1 = "~/path1";
  public static final String PATH_2 = "./path2";
  @Inject ProtobufStepSerializer<SaveCacheStepInfo> protobufSerializer;
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldSerializeSaveCache() throws InvalidProtocolBufferException {
    SaveCacheStepInfo saveCacheStepInfo = SaveCacheStepInfo.builder()
                                              .name(SAVE_CACHE)
                                              .identifier(SAVE_CACHE_ID)
                                              .key(SAVE_CACHE_KEY)
                                              .paths(Arrays.asList(PATH_1, PATH_2))
                                              .build();
    saveCacheStepInfo.setCallbackId(CALLBACK_ID);
    String serialize = protobufSerializer.serializeToBase64(saveCacheStepInfo);
    UnitStep saveCacheStep = UnitStep.parseFrom(Base64.decodeBase64(serialize));

    assertThat(saveCacheStep.getDisplayName()).isEqualTo(SAVE_CACHE);
    assertThat(saveCacheStep.getId()).isEqualTo(SAVE_CACHE_ID);
    assertThat(saveCacheStep.getSaveCache().getKey()).isEqualTo(SAVE_CACHE_KEY);
    assertThat(saveCacheStep.getTaskId()).isEqualTo(CALLBACK_ID);
    assertThat(saveCacheStep.getSaveCache().getPaths(0)).isEqualTo(PATH_1);
    assertThat(saveCacheStep.getSaveCache().getPaths(1)).isEqualTo(PATH_2);
  }
}
