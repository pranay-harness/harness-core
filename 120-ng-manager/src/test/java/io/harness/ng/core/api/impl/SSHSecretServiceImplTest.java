package io.harness.ng.core.api.impl;

import static io.harness.rule.OwnerRule.PHOENIKX;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.SSHAuthDTO;
import io.harness.ng.core.dto.secrets.SSHConfigDTO;
import io.harness.ng.core.dto.secrets.SSHCredentialType;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SSHPasswordCredentialDTO;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.SSHAuthScheme;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.remote.SecretManagerClient;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Response;

public class SSHSecretServiceImplTest extends CategoryTest {
  private SecretManagerClient secretManagerClient;
  private SSHSecretServiceImpl sshSecretService;

  @Before
  public void setup() {
    secretManagerClient = mock(SecretManagerClient.class, RETURNS_DEEP_STUBS);
    sshSecretService = new SSHSecretServiceImpl(secretManagerClient);
  }

  private SecretDTOV2 getBaseSecret() {
    return SecretDTOV2.builder()
        .name("name")
        .type(SecretType.SSHKey)
        .identifier("identifier")
        .tags(Maps.newHashMap(ImmutableMap.of("a", "b")))
        .build();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateSSHKey() throws IOException {
    SecretDTOV2 secretDTOV2 = getBaseSecret();
    secretDTOV2.setSpec(
        SSHKeySpecDTO.builder()
            .auth(
                SSHAuthDTO.builder()
                    .type(SSHAuthScheme.SSH)
                    .spec(
                        SSHConfigDTO.builder()
                            .credentialType(SSHCredentialType.Password)
                            .spec(SSHPasswordCredentialDTO.builder()
                                      .userName("username")
                                      .password(
                                          SecretRefData.builder().identifier("identifier").scope(Scope.ACCOUNT).build())
                                      .build())
                            .build())
                    .build())
            .port(22)
            .build());
    when(secretManagerClient.updateSecretFile(any(), any(), any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(true)));
    boolean success = sshSecretService.update("Account", secretDTOV2);
    assertThat(success).isTrue();
    verify(secretManagerClient, atLeastOnce()).updateSecretFile(any(), any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateViaYaml() throws IOException {
    SecretDTOV2 secretDTOV2 = getBaseSecret();
    secretDTOV2.setSpec(
        SSHKeySpecDTO.builder()
            .auth(
                SSHAuthDTO.builder()
                    .type(SSHAuthScheme.SSH)
                    .spec(
                        SSHConfigDTO.builder()
                            .credentialType(SSHCredentialType.Password)
                            .spec(SSHPasswordCredentialDTO.builder()
                                      .userName("username")
                                      .password(
                                          SecretRefData.builder().identifier("identifier").scope(Scope.ACCOUNT).build())
                                      .build())
                            .build())
                    .build())
            .port(22)
            .build());
    when(secretManagerClient.updateSecretFile(any(), any(), any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(true)));
    boolean success = sshSecretService.updateViaYaml("Account", secretDTOV2);
    assertThat(success).isTrue();
    verify(secretManagerClient, atLeastOnce()).updateSecretFile(any(), any(), any(), any(), any(), any());
  }
}
