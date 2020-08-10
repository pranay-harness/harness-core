package io.harness.ng.core.api;

import static io.harness.secretmanagerclient.utils.SecretManagerClientUtils.getResponse;
import static software.wings.resources.secretsmanagement.EncryptedDataMapper.fromDTO;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.serializer.JsonUtils;
import io.harness.stream.BoundedInputStream;
import lombok.AllArgsConstructor;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NGSecretFileServiceImpl implements NGSecretFileService {
  private final SecretManagerClient secretManagerClient;

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
  public EncryptedData create(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, String secretManagerIdentifier, String name, String description, List<String> tags,
      BoundedInputStream inputStream) {
    try {
      return fromDTO(getResponse(secretManagerClient.createSecretFile(getRequestBody(name),
          getRequestBody(JsonUtils.asJson(tags)), getRequestBody(description), getRequestBody(accountIdentifier),
          getRequestBody(orgIdentifier), getRequestBody(projectIdentifier), getRequestBody(identifier),
          getRequestBody(secretManagerIdentifier), getRequestBody(ByteStreams.toByteArray(inputStream)))));
    } catch (IOException exception) {
      throw new SecretManagementException("Error while converting file to bytes");
    }
  }

  @Override
  public boolean update(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      String description, List<String> tags, BoundedInputStream inputStream) {
    try {
      return getResponse(secretManagerClient.updateSecretFile(identifier, getRequestBody(JsonUtils.asJson(tags)),
          getRequestBody(description), getRequestBody(accountIdentifier), getRequestBody(orgIdentifier),
          getRequestBody(projectIdentifier), getRequestBody(ByteStreams.toByteArray(inputStream))));
    } catch (IOException exception) {
      throw new SecretManagementException("Error while converting file to bytes");
    }
  }
}
