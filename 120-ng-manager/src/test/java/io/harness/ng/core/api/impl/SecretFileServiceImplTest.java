package io.harness.ng.core.api.impl;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Response;

import java.io.IOException;

public class SecretFileServiceImplTest extends CategoryTest {
  private SecretManagerClient secretManagerClient;
  private SecretFileServiceImpl secretFileService;

  @Before
  public void setup() {
    secretManagerClient = mock(SecretManagerClient.class, RETURNS_DEEP_STUBS);
    secretFileService = new SecretFileServiceImpl(secretManagerClient);
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
  public void testCreate() throws IOException {
    EncryptedDataDTO encryptedDataDTO = random(EncryptedDataDTO.class);
    when(secretManagerClient.createSecretFile(any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(encryptedDataDTO)));

    SecretDTOV2 secretDTO = getBaseSecret();
    secretDTO.setSpec(SecretFileSpecDTO.builder().secretManagerIdentifier("sm").build());
    EncryptedDataDTO savedData = secretFileService.create("account", secretDTO);

    assertThat(savedData).isEqualTo(encryptedDataDTO);
    verify(secretManagerClient, atLeastOnce()).createSecretFile(any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdate() throws IOException {
    EncryptedDataDTO encryptedDataDTO = random(EncryptedDataDTO.class);
    encryptedDataDTO.setSecretManager("sm");
    when(secretManagerClient.getSecret(any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(encryptedDataDTO)));
    when(secretManagerClient.updateSecretFile(any(), any(), any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(true)));

    SecretDTOV2 secretDTO = getBaseSecret();
    secretDTO.setSpec(SecretFileSpecDTO.builder().secretManagerIdentifier("sm").build());
    boolean success = secretFileService.update("account", secretDTO);

    assertThat(success).isEqualTo(true);
    verify(secretManagerClient, atLeastOnce()).updateSecretFile(any(), any(), any(), any(), any(), any());
  }
}
