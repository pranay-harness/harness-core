package io.harness.ng.core.remote;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.ng.core.remote.EncryptedDataMapper.writeDTO;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.ResponseDTO;
import io.harness.ng.core.dto.EncryptedDataDTO;
import io.harness.ng.core.services.api.NGSecretService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.impl.security.SecretText;

public class NGSecretResourceTest extends BaseTest {
  private NGSecretService ngSecretService;
  private NGSecretResource ngSecretResource;

  @Before
  public void setup() {
    ngSecretService = mock(NGSecretService.class);
    ngSecretResource = new NGSecretResource(ngSecretService);
  }
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String SECRET_ID = "SECRET_ID";
  private final String SECRET_NAME = "SECRET_NAME";
  private final String UUID = "UUID";

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetSecret() {
    EncryptedData encryptedData = EncryptedData.builder().name(SECRET_NAME).build();
    EncryptedDataDTO encryptedDataDTO = writeDTO(encryptedData);
    when(ngSecretService.getSecretById(anyString(), eq(SECRET_ID))).thenReturn(encryptedData);
    encryptedDataDTO = ngSecretResource.get(SECRET_ID, ACCOUNT_ID).getData();
    assertThat(encryptedDataDTO).isNotNull();
    assertThat(encryptedDataDTO.getName()).isEqualTo(SECRET_NAME);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetSecret_For_Exception() {
    when(ngSecretService.getSecretById(anyString(), eq(SECRET_ID)))
        .thenThrow(new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "IOException exception occurred while fetching secret", USER));
    boolean exceptionThrown = false;
    try {
      ngSecretResource.get(SECRET_ID, ACCOUNT_ID);
    } catch (SecretManagementException sme) {
      exceptionThrown = true;
      assertThat(sme.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateSecret() {
    String secretEncryptionId = randomAlphabetic(10);
    SecretText randomSecretText = random(SecretText.class);

    when(ngSecretService.createSecret(eq(ACCOUNT_ID), anyBoolean(), eq(randomSecretText)))
        .thenReturn(secretEncryptionId);

    ResponseDTO<String> createSecretDTO = ngSecretResource.createSecret(ACCOUNT_ID, true, randomSecretText);
    assertThat(createSecretDTO).isNotNull();
    assertThat(createSecretDTO.getData()).isEqualTo(secretEncryptionId);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateSecret_For_Exception() {
    SecretText randomSecretText = random(SecretText.class);

    when(ngSecretService.createSecret(eq(ACCOUNT_ID), anyBoolean(), eq(randomSecretText)))
        .thenThrow(new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "IOException exception occured while saving the secret", USER));

    boolean exceptionThrown = false;
    try {
      ngSecretResource.createSecret(ACCOUNT_ID, true, randomSecretText);
    } catch (SecretManagementException ex) {
      exceptionThrown = true;
      assertThat(ex.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateSecret() {
    SecretText randomSecretText = random(SecretText.class);

    when(ngSecretService.updateSecret(ACCOUNT_ID, UUID, randomSecretText)).thenReturn(true);

    ResponseDTO<Boolean> updateSecretDTO = ngSecretResource.updateSecret(ACCOUNT_ID, UUID, randomSecretText);
    assertThat(updateSecretDTO).isNotNull();
    assertThat(updateSecretDTO.getData()).isEqualTo(true);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateSecret_For_Exception() {
    SecretText randomSecretText = random(SecretText.class);

    when(ngSecretService.updateSecret(ACCOUNT_ID, UUID, randomSecretText))
        .thenThrow(new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "IOException exception occured while updating the secret", USER));

    boolean exceptionThrown = false;
    try {
      ngSecretResource.updateSecret(ACCOUNT_ID, UUID, randomSecretText);
    } catch (SecretManagementException ex) {
      exceptionThrown = true;
      assertThat(ex.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteSecret() {
    when(ngSecretService.deleteSecret(ACCOUNT_ID, UUID)).thenReturn(true);

    ResponseDTO<Boolean> deleteSecretDTO = ngSecretResource.deleteSecret(ACCOUNT_ID, UUID);
    assertThat(deleteSecretDTO).isNotNull();
    assertThat(deleteSecretDTO.getData()).isEqualTo(true);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteSecret_For_Exception() {
    when(ngSecretService.deleteSecret(ACCOUNT_ID, UUID))
        .thenThrow(new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "IOException exception occured while deleting the secret", USER));

    boolean exceptionThrown = false;
    try {
      ngSecretResource.deleteSecret(ACCOUNT_ID, UUID);
    } catch (SecretManagementException ex) {
      exceptionThrown = true;
      assertThat(ex.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }
}
