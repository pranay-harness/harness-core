package io.harness.resourcegroup.resource.client.secretmanager;

import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.resourcegroup.resource.validator.ResourceValidator;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
public class SecretManagerResourceValidatorImpl implements ResourceValidator {
  SecretManagerClient secretManagerClient;

  @Override
  public boolean validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<SecretManagerConfigDTO> secretManagers = getResponse(
        secretManagerClient.listSecretManagers(accountIdentifier, orgIdentifier, projectIdentifier, resourceIds));
    return secretManagers.size() == resourceIds.size();
  }
}
