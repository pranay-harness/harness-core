package io.harness.beans.serializer;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CiBeansTestBase;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.rule.Owner;
import io.harness.yaml.core.StepElement;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RestoreCacheStepProtobufSerializerTest extends CiBeansTestBase {
  public static final String RESTORE_ID = "restore-id";
  public static final String RESTORE_CACHE = "restore-cache";
  public static final String RESTORE_KEY = "restore-key";
  public static final String CALLBACK_ID = "callbackId";
  @Inject ProtobufStepSerializer<RestoreCacheStepInfo> protobufSerializer;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldSerializeRestoreCacheStep() throws InvalidProtocolBufferException {
    RestoreCacheStepInfo restoreCacheStepInfo = RestoreCacheStepInfo.builder()
                                                    .name(RESTORE_CACHE)
                                                    .identifier(RESTORE_ID)
                                                    .key(RESTORE_KEY)
                                                    .failIfNotExist(true)
                                                    .build();
    StepElement stepElement = StepElement.builder().type("restoreCache").stepSpecType(restoreCacheStepInfo).build();

    restoreCacheStepInfo.setCallbackId(CALLBACK_ID);
    String serialize = protobufSerializer.serializeToBase64(stepElement);
    UnitStep restoreStep = UnitStep.parseFrom(Base64.decodeBase64(serialize));

    assertThat(restoreStep.getRestoreCache().getKey()).isEqualTo(RESTORE_KEY);
    assertThat(restoreStep.getDisplayName()).isEqualTo(RESTORE_CACHE);
    assertThat(restoreStep.getId()).isEqualTo(RESTORE_ID);
    assertThat(restoreStep.getTaskId()).isEqualTo(CALLBACK_ID);
    assertThat(restoreStep.getRestoreCache().getFailIfNotExist()).isTrue();
  }
}
