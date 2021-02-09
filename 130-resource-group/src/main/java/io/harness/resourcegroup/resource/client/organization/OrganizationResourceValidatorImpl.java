package io.harness.resourcegroup.resource.client.organization;

import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.organizationmanagerclient.remote.OrganizationManagerClient;
import io.harness.resourcegroup.resource.validator.ResourceValidator;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
public class OrganizationResourceValidatorImpl implements ResourceValidator {
  OrganizationManagerClient organizationManagerClient;

  @Override
  public boolean validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    PageResponse<OrganizationResponse> organizations =
        getResponse(organizationManagerClient.listOrganizations(accountIdentifier, resourceIds));
    return organizations.getContent().size() == resourceIds.size();
  }
}
