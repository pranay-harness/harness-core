package io.harness.beans.seriazlier;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.product.ci.engine.proto.Step;
import io.harness.rule.Owner;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RestoreCacheStepProtobufSerializerTest extends CIBeansTest {
  public static final String RESTORE_ID = "restore-id";
  public static final String RESTORE_CACHE = "restore-cache";
  public static final String RESTORE_KEY = "restore-key";
  @Inject ProtobufSerializer<RestoreCacheStepInfo> protobufSerializer;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldSerializeRestoreCacheStep() throws InvalidProtocolBufferException {
    RestoreCacheStepInfo restoreCacheStepInfo =
        RestoreCacheStepInfo.builder()
            .displayName(RESTORE_CACHE)
            .identifier(RESTORE_ID)
            .restoreCache(RestoreCacheStepInfo.RestoreCache.builder().key(RESTORE_KEY).failIfNotExist(true).build())
            .build();
    String serialize = protobufSerializer.serialize(restoreCacheStepInfo);
    Step restoreStep = Step.parseFrom(Base64.decodeBase64(serialize));

    assertThat(restoreStep.getRestoreCache().getKey()).isEqualTo(RESTORE_KEY);
    assertThat(restoreStep.getDisplayName()).isEqualTo(RESTORE_CACHE);
    assertThat(restoreStep.getId()).isEqualTo(RESTORE_ID);
    assertThat(restoreStep.getRestoreCache().getFailIfNotExist()).isTrue();
  }
}