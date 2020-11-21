package io.harness.ng.core.remote;

import static io.harness.NGConstants.HARNESS_BLUE;

import static java.util.Collections.emptyList;

import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.entities.Organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrganizationMapper {
  public static Organization toOrganization(OrganizationDTO dto) {
    return Organization.builder()
        .accountIdentifier(dto.getAccountIdentifier())
        .tags(Optional.ofNullable(dto.getTags()).orElse(emptyList()))
        .color(Optional.ofNullable(dto.getColor()).orElse(HARNESS_BLUE))
        .description(Optional.ofNullable(dto.getDescription()).orElse(""))
        .identifier(dto.getIdentifier())
        .name(dto.getName())
        .build();
  }

  public static OrganizationDTO writeDto(Organization organization) {
    return OrganizationDTO.builder()
        .color(organization.getColor())
        .description(organization.getDescription())
        .identifier(organization.getIdentifier())
        .accountIdentifier(organization.getAccountIdentifier())
        .name(organization.getName())
        .tags(organization.getTags())
        .lastModifiedAt(organization.getLastModifiedAt())
        .build();
  }

  @SneakyThrows
  public static Organization applyUpdateToOrganization(
      Organization organization, OrganizationDTO updateOrganizationDTO) {
    String jsonString = new ObjectMapper().writer().writeValueAsString(updateOrganizationDTO);
    return new ObjectMapper().readerForUpdating(organization).readValue(jsonString);
  }
}
