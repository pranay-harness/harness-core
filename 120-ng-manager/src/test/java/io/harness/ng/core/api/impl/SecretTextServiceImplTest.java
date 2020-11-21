package io.harness.ng.core.api.impl;

import static io.harness.rule.OwnerRule.PHOENIKX;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Response;

public class SecretTextServiceImplTest extends CategoryTest {
  private SecretManagerClient secretManagerClient;
  private SecretTextServiceImpl secretTextService;

  @Before
  public void setup() {
    secretManagerClient = mock(SecretManagerClient.class, RETURNS_DEEP_STUBS);
    secretTextService = new SecretTextServiceImpl(secretManagerClient);
  }

  private SecretDTOV2 getBaseSecret() {
    return SecretDTOV2.builder()
        .name("name")
        .type(SecretType.SecretText)
        .identifier("identifier")
        .tags(Maps.newHashMap(ImmutableMap.of("a", "b")))
        .build();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretText() throws IOException {
    SecretDTOV2 secretDTOV2 = getBaseSecret();
    EncryptedDataDTO encryptedDataDTO = random(EncryptedDataDTO.class);
    secretDTOV2.setSpec(
        SecretTextSpecDTO.builder().secretManagerIdentifier("sm").valueType(ValueType.Inline).value("value").build());
    when(secretManagerClient.createSecret(any()).execute())
        .thenReturn(Response.success(new RestResponse<>(encryptedDataDTO)));

    EncryptedDataDTO savedData = secretTextService.create("account", secretDTOV2);

    assertThat(savedData).isNotNull();
    assertThat(savedData).isEqualTo(encryptedDataDTO);
    verify(secretManagerClient, atLeastOnce()).createSecret(any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateSecretText() throws IOException {
    SecretDTOV2 secretDTOV2 = getBaseSecret();
    EncryptedDataDTO encryptedDataDTO = random(EncryptedDataDTO.class);
    encryptedDataDTO.setSecretManager("sm");
    secretDTOV2.setSpec(
        SecretTextSpecDTO.builder().secretManagerIdentifier("sm").valueType(ValueType.Inline).value("value").build());
    when(secretManagerClient.getSecret(any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(encryptedDataDTO)));
    when(secretManagerClient.updateSecret(any(), any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(true)));

    boolean success = secretTextService.update("account", secretDTOV2);
    assertThat(success).isNotNull();
    verify(secretManagerClient, atLeastOnce()).getSecret(any(), any(), any(), any());
    verify(secretManagerClient, atLeastOnce()).updateSecret(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateViaYaml() throws IOException {
    SecretDTOV2 secretDTOV2 = getBaseSecret();
    EncryptedDataDTO encryptedDataDTO = random(EncryptedDataDTO.class);
    encryptedDataDTO.setSecretManager("sm");
    secretDTOV2.setSpec(
        SecretTextSpecDTO.builder().secretManagerIdentifier("sm").valueType(ValueType.Inline).value("value").build());
    when(secretManagerClient.getSecret(any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(encryptedDataDTO)));
    when(secretManagerClient.updateSecret(any(), any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(true)));

    boolean success = secretTextService.updateViaYaml("account", secretDTOV2);
    assertThat(success).isNotNull();
    verify(secretManagerClient, atLeastOnce()).getSecret(any(), any(), any(), any());
    verify(secretManagerClient, atLeastOnce()).updateSecret(any(), any(), any(), any(), any());
  }
}
