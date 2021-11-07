package io.harness.secretkey;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PL)
public class AESSecretKeyServiceTest extends CategoryTest {
  @Inject SecretKeyService aesSecretKeyService;

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testCreateAESSecretKeyAlgorithm() {
    assertThat(aesSecretKeyService.getAlgorithm()).isEqualTo(SecretKeyConstants.AES_ENCRYPTION_ALGORITHM);
  }
}
