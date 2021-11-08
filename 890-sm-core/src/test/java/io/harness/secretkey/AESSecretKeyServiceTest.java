package io.harness.secretkey;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.repositories.SecretKeyRepository;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PL)
public class AESSecretKeyServiceTest extends CategoryTest {
  @InjectMocks AESSecretKeyServiceImpl aesSecretKeyService;
  @Mock SecretKeyRepository secretKeyRepository;

  @Before
  public void beforeClass() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testCreateAESSecretKeyAlgorithm() {
    assertThat(aesSecretKeyService.getAlgorithm()).isEqualTo(SecretKeyConstants.AES_ENCRYPTION_ALGORITHM);
  }
}
