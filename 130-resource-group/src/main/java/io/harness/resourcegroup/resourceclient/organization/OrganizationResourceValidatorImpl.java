package io.harness.resourcegroup.resourceclient.organization;

import static io.harness.remote.client.NGRestUtils.getResponse;

import static java.util.stream.Collectors.toList;

import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.organizationmanagerclient.remote.OrganizationManagerClient;
import io.harness.resourcegroup.model.Scope;
import io.harness.resourcegroup.resourceclient.api.ResourceValidator;

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
public class OrganizationResourceValidatorImpl implements ResourceValidator {
  OrganizationManagerClient organizationManagerClient;

  @Override
  public List<Boolean> validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    PageResponse<OrganizationResponse> organizations =
        getResponse(organizationManagerClient.listOrganizations(accountIdentifier, resourceIds));
    Set<String> validResourcIds =
        organizations.getContent().stream().map(e -> e.getOrganization().getIdentifier()).collect(Collectors.toSet());
    return resourceIds.stream().map(validResourcIds::contains).collect(toList());
  }

  @Override
  public String getResourceType() {
    return "ORGANIZATION";
  }

  @Override
  public Set<Scope> getScopes() {
    return EnumSet.of(Scope.ACCOUNT);
  }
}
