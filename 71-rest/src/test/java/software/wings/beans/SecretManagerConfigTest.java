package software.wings.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.SecretManagerConfig.SecretManagerConfigKeys;

import java.security.SecureRandom;

public class SecretManagerConfigTest extends CategoryTest {
  private static final SecureRandom random = new SecureRandom();

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testSecretManagerRenewalJobIteration() throws IllegalAccessException {
    VaultConfig vaultConfig = VaultConfig.builder().name("SM").build();
    vaultConfig.setUuid("uuid");
    vaultConfig.setEncryptionType(EncryptionType.VAULT);
    long nextTokenRenewIteration = random.nextLong();
    FieldUtils.writeField(vaultConfig, SecretManagerConfigKeys.nextTokenRenewIteration, nextTokenRenewIteration, true);
    assertThat(vaultConfig.obtainNextIteration(SecretManagerConfigKeys.nextTokenRenewIteration))
        .isEqualTo(nextTokenRenewIteration);

    nextTokenRenewIteration = random.nextLong();
    vaultConfig.updateNextIteration(SecretManagerConfigKeys.nextTokenRenewIteration, nextTokenRenewIteration);
    assertThat(vaultConfig.obtainNextIteration(SecretManagerConfigKeys.nextTokenRenewIteration))
        .isEqualTo(nextTokenRenewIteration);

    try {
      vaultConfig.updateNextIteration(generateUuid(), random.nextLong());
      fail("Did not throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      vaultConfig.obtainNextIteration(generateUuid());
      fail("Did not throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
}
