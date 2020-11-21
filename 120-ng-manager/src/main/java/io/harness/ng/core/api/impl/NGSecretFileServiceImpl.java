package io.harness.ng.core.api.impl;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.SRE;
import static io.harness.ng.core.utils.NGUtils.verifyValuesNotChanged;
import static io.harness.remote.client.RestClientUtils.getResponse;

import static software.wings.resources.secretsmanagement.EncryptedDataMapper.fromDTO;

import io.harness.beans.EncryptedData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.ng.core.api.NGSecretFileService;
import io.harness.ng.core.api.NGSecretService;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.secretmanagerclient.dto.SecretFileUpdateDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.serializer.JsonUtils;
import io.harness.stream.BoundedInputStream;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGSecretFileServiceImpl implements NGSecretFileService {
  private final SecretManagerClient secretManagerClient;
  private final NGSecretService ngSecretService;

  private RequestBody getRequestBody(String value) {
    if (!Optional.ofNullable(value).isPresent()) {
      return null;
    }
    return RequestBody.create(MediaType.parse("text/plain"), value);
  }

  private RequestBody getRequestBody(byte[] bytes) {
    return RequestBody.create(MediaType.parse("text/plain"), bytes);
  }

  @Override
  public EncryptedData create(SecretFileDTO dto, BoundedInputStream inputStream) {
    try {
      return fromDTO(getResponse(secretManagerClient.createSecretFile(getRequestBody(JsonUtils.asJson(dto)),
          inputStream != null ? getRequestBody(ByteStreams.toByteArray(inputStream)) : null)));
    } catch (IOException exception) {
      log.error("Error while converting file to bytes for request {}", dto);
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Error while converting file to bytes", SRE);
    }
  }

  @SneakyThrows
  @Override
  public boolean update(SecretFileDTO dto, BoundedInputStream inputStream) {
    EncryptedData encryptedData =
        ngSecretService.get(dto.getAccount(), dto.getOrg(), dto.getProject(), dto.getIdentifier());
    if (Optional.ofNullable(encryptedData).isPresent()) {
      verifyValuesNotChanged(
          Lists.newArrayList(Pair.of(dto.getType(), SecretType.fromSettingVariableType(encryptedData.getType())),
              Pair.of(dto.getSecretManager(), encryptedData.getNgMetadata().getSecretManagerIdentifier())));
      SecretFileUpdateDTO updateDTO = SecretFileUpdateDTO.builder()
                                          .description(dto.getDescription())
                                          .name(dto.getName())
                                          .tags(dto.getTags())
                                          .build();
      return getResponse(secretManagerClient.updateSecretFile(dto.getIdentifier(), dto.getAccount(), dto.getOrg(),
          dto.getProject(), inputStream != null ? getRequestBody(ByteStreams.toByteArray(inputStream)) : null,
          getRequestBody(JsonUtils.asJson(updateDTO))));
    }
    throw new InvalidRequestException("No such secret file found.");
  }
}
