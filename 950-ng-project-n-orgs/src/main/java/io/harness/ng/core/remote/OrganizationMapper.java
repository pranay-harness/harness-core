package io.harness.ng.core.remote;

import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationResponse;
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
        .tags(convertToList(dto.getTags()))
        .description(Optional.ofNullable(dto.getDescription()).orElse(""))
        .identifier(dto.getIdentifier())
        .name(dto.getName())
        .harnessManaged(dto.isHarnessManaged())
        .version(dto.getVersion())
        .build();
  }

  public static OrganizationDTO writeDto(Organization organization) {
    return OrganizationDTO.builder()
        .description(organization.getDescription())
        .identifier(organization.getIdentifier())
        .accountIdentifier(organization.getAccountIdentifier())
        .name(organization.getName())
        .tags(convertToMap(organization.getTags()))
        .build();
  }

  public static OrganizationResponse toResponseWrapper(Organization organization) {
    return OrganizationResponse.builder()
        .createdAt(organization.getCreatedAt())
        .lastModifiedAt(organization.getLastModifiedAt())
        .harnessManaged(Boolean.TRUE.equals(organization.getHarnessManaged()))
        .organization(writeDto(organization))
        .build();
  }

  @SneakyThrows
  public static Organization applyUpdateToOrganization(
      Organization organization, OrganizationDTO updateOrganizationDTO) {
    String jsonString = new ObjectMapper().writer().writeValueAsString(updateOrganizationDTO);
    return new ObjectMapper().readerForUpdating(organization).readValue(jsonString);
  }
}
