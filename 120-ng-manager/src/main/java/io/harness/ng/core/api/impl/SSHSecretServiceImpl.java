package io.harness.ng.core.api.impl;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.secretmanagerclient.dto.SecretFileUpdateDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.serializer.JsonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class SSHSecretServiceImpl implements SecretModifyService {
  private final SecretManagerClient secretManagerClient;

  @Override
  public EncryptedDataDTO create(String accountIdentifier, SecretDTOV2 dto) {
    SecretFileDTO secretFileDTO = SecretFileDTO.builder()
                                      .name(dto.getName())
                                      .account(accountIdentifier)
                                      .org(dto.getOrgIdentifier())
                                      .project(dto.getProjectIdentifier())
                                      .description(dto.getDescription())
                                      .identifier(dto.getIdentifier())
                                      .tags(null)
                                      .type(SecretType.SSHKey)
                                      .secretManager(HARNESS_SECRET_MANAGER_IDENTIFIER)
                                      .build();
    return getResponse(secretManagerClient.createSecretFile(getRequestBody(JsonUtils.asJson(secretFileDTO)), null));
  }

  @Override
  public boolean update(String accountIdentifier, SecretDTOV2 dto) {
    SecretFileUpdateDTO updateDTO =
        SecretFileUpdateDTO.builder().name(dto.getName()).tags(null).description(dto.getDescription()).build();
    return getResponse(secretManagerClient.updateSecretFile(dto.getIdentifier(), accountIdentifier,
        dto.getOrgIdentifier(), dto.getProjectIdentifier(), null, getRequestBody(JsonUtils.asJson(updateDTO))));
  }

  @Override
  public boolean updateViaYaml(String accountIdentifier, SecretDTOV2 dto) {
    return update(accountIdentifier, dto);
  }
}
