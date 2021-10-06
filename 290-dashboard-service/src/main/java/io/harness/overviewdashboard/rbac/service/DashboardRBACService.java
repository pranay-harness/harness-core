package io.harness.overviewdashboard.rbac.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.ng.core.dto.ProjectDTO;

import java.util.List;
import java.util.Map;

@OwnedBy(PL)
public interface DashboardRBACService {
  List<ProjectDTO> listAccessibleProject(String accountIdentifier, String userId);

  PageResponse<OrganizationResponse> listAccessibleOrganizations(String accountIdentifier, List<String> orgIdentifiers);

  Map<String, String> getMapOfOrganizationIdentifierAndOrganizationName(
      String accountIdentifier, List<String> orgIdentifiers);
}
