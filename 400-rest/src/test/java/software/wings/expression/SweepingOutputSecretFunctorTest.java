/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.expression;

import static io.harness.rule.OwnerRule.HINGER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.data.encoding.EncodingUtils;
import io.harness.rule.Owner;
import io.harness.security.SimpleEncryption;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SweepingOutputSecretFunctorTest extends WingsBaseTest {
  private static final String SECRET_VALUE = "secretValue";
  private static final String SECRET_NAME = "secretValue";
  @Inject SimpleEncryption simpleEncryption;

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void testObtainSecretWhenDryRun() {
    SweepingOutputSecretFunctor sweepingOutputSecretFunctor = SweepingOutputSecretFunctor.builder()
                                                                  .mode(SecretManagerMode.DRY_RUN)
                                                                  .simpleEncryption(simpleEncryption)
                                                                  .build();
    String encryptedSecretValue =
        EncodingUtils.encodeBase64(simpleEncryption.encrypt(SECRET_VALUE.getBytes(StandardCharsets.UTF_8)));
    assertThat(sweepingOutputSecretFunctor.obtain(SECRET_NAME, encryptedSecretValue))
        .isEqualTo("${sweepingOutputSecrets.obtain(\"" + SECRET_NAME + "\",\"" + encryptedSecretValue + "\")}");
    assertThat(sweepingOutputSecretFunctor.getEvaluatedSecrets().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void testObtainSecretWhenApply() {
    SweepingOutputSecretFunctor sweepingOutputSecretFunctor =
        SweepingOutputSecretFunctor.builder().mode(SecretManagerMode.APPLY).simpleEncryption(simpleEncryption).build();
    String encryptedSecretValue =
        EncodingUtils.encodeBase64(simpleEncryption.encrypt(SECRET_VALUE.getBytes(StandardCharsets.UTF_8)));

    assertThat(sweepingOutputSecretFunctor.obtain(SECRET_NAME, encryptedSecretValue)).isEqualTo(SECRET_VALUE);
    assertThat(sweepingOutputSecretFunctor.getEvaluatedSecrets().size()).isEqualTo(1);
  }
}
