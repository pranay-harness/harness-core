package io.harness.ng.core.remote;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.dto.SecretDTO;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.intfc.security.SecretManager;

public class NGSecretManagerResourceTest extends BaseTest {
  @Mock SecretManager secretManager;
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String SECRET_ID = "SECRET_ID";
  private final String SECRET_NAME = "SECRET_NAME";
  private NGSecretManagerResource ngSecretManagerResource;

  @Before
  public void doSetup() {
    ngSecretManagerResource = new NGSecretManagerResource(secretManager);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetSecret() {
    NGSecretManagerResource ngSecretManagerResource = new NGSecretManagerResource(secretManager);
    EncryptedData encryptedData = EncryptedData.builder().name(SECRET_NAME).build();
    when(secretManager.getSecretById(anyString(), eq(SECRET_ID))).thenReturn(encryptedData);
    SecretDTO getSecretDTO = ngSecretManagerResource.get(SECRET_ID, ACCOUNT_ID);
    assertThat(getSecretDTO).isNotNull();
    assertThat(getSecretDTO.getName()).isEqualTo(SECRET_NAME);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetSecret_For_Exception() {
    NGSecretManagerResource ngSecretManagerResource = new NGSecretManagerResource(secretManager);
    when(secretManager.getSecretById(anyString(), eq(SECRET_ID)))
        .thenThrow(new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "IOException exception occurred while fetching secret", USER));
    boolean exceptionThrown = false;
    try {
      ngSecretManagerResource.get(SECRET_ID, ACCOUNT_ID);
    } catch (SecretManagementException sme) {
      exceptionThrown = true;
      assertThat(sme.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }
}
