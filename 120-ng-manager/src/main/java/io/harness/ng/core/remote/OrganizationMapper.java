package io.harness.ng.core.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.ng.core.dto.CreateOrganizationDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.UpdateOrganizationDTO;
import io.harness.ng.core.entities.Organization;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrganizationMapper {
  static Organization toOrganization(CreateOrganizationDTO createOrgRequest) {
    return Organization.builder()
        .accountId(createOrgRequest.getAccountId())
        .tags(createOrgRequest.getTags())
        .color(createOrgRequest.getColor())
        .description(createOrgRequest.getDescription())
        .identifier(createOrgRequest.getIdentifier())
        .name(createOrgRequest.getName())
        .build();
  }

  static OrganizationDTO writeDto(Organization organization) {
    return OrganizationDTO.builder()
        .id(organization.getId())
        .accountId(organization.getAccountId())
        .color(organization.getColor())
        .description(organization.getDescription())
        .identifier(organization.getIdentifier())
        .name(organization.getName())
        .tags(organization.getTags())
        .build();
  }

  @SneakyThrows
  static Organization applyUpdateToOrganization(
      Organization organization, UpdateOrganizationDTO updateOrganizationDTO) {
    String jsonString = new ObjectMapper().writer().writeValueAsString(updateOrganizationDTO);
    return new ObjectMapper().readerForUpdating(organization).readValue(jsonString);
  }
}
