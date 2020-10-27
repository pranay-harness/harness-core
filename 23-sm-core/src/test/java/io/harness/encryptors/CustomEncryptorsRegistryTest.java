package io.harness.encryptors;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.security.encryption.EncryptionType.CUSTOM;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.inject.Inject;

import io.harness.SMCoreTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CustomEncryptorsRegistryTest extends SMCoreTestBase {
  @Inject CustomEncryptorsRegistry customEncryptorsRegistry;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetCustomEncryptor_shouldReturn() {
    CustomEncryptor customEncryptor = customEncryptorsRegistry.getCustomEncryptor(CUSTOM);
    assertThat(customEncryptor).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetCustomEncryptor_shouldThrowError() {
    try {
      CustomEncryptor customEncryptor = customEncryptorsRegistry.getCustomEncryptor(GCP_KMS);
      fail("The method should have thrown an error");
    } catch (SecretManagementDelegateException e) {
      assertThat(e.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
  }
}
