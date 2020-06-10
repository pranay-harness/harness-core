package io.harness.ng.core.remote;

import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.ng.core.dto.CreateOrganizationRequest;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.UpdateOrganizationRequest;
import io.harness.ng.core.entities.Organization;
import lombok.SneakyThrows;

@Singleton
public final class OrganizationMapper {
  Organization toOrganization(CreateOrganizationRequest createOrgRequest) {
    return Organization.builder()
        .accountId(createOrgRequest.getAccountId())
        .tags(createOrgRequest.getTags())
        .color(createOrgRequest.getColor())
        .description(createOrgRequest.getDescription())
        .identifier(createOrgRequest.getIdentifier())
        .name(createOrgRequest.getName())
        .build();
  }

  OrganizationDTO writeDto(Organization organization) {
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
  Organization applyUpdateToOrganization(Organization organization, UpdateOrganizationRequest request) {
    String jsonString = new ObjectMapper().writer().writeValueAsString(request);
    return new ObjectMapper().readerForUpdating(organization).readValue(jsonString);
  }
}
