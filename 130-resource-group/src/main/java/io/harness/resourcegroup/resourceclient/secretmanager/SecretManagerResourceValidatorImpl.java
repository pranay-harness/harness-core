package io.harness.resourcegroup.resourceclient.secretmanager;

import static io.harness.remote.client.RestClientUtils.getResponse;

import static java.util.stream.Collectors.toList;

import io.harness.resourcegroup.model.Scope;
import io.harness.resourcegroup.resourceclient.api.ResourceValidator;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;

import com.google.inject.Inject;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
public class SecretManagerResourceValidatorImpl implements ResourceValidator {
  SecretManagerClient secretManagerClient;

  @Override
  public List<Boolean> validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<SecretManagerConfigDTO> secretManagers = getResponse(
        secretManagerClient.listSecretManagers(accountIdentifier, orgIdentifier, projectIdentifier, resourceIds));
    Set<String> validResourcIds =
        secretManagers.stream().map(SecretManagerConfigDTO::getIdentifier).collect(Collectors.toSet());
    return resourceIds.stream().map(validResourcIds::contains).collect(toList());
  }

  @Override
  public String getResourceType() {
    return "SECRET_MANAGER";
  }

  @Override
  public Set<Scope> getScopes() {
    return EnumSet.of(Scope.ACCOUNT, Scope.ORGANIZATION, Scope.PROJECT);
  }
}
