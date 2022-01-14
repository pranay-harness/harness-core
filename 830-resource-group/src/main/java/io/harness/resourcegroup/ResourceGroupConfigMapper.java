package io.harness.resourcegroup;

import static io.harness.NGConstants.HARNESS_BLUE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class ResourceGroupConfigMapper {
  public ResourceGroupDTO toDTO(ResourceGroupConfig config) {
    ResourceGroupDTO dto = ResourceGroupDTO.builder()
                               .identifier(config.getIdentifier())
                               .name(config.getName())
                               .fullScopeSelected(config.isFullScopeSelected())
                               .tags(config.getTags())
                               .resourceSelectors(config.getResourceSelectors())
                               .description(config.getDescription())
                               .color(HARNESS_BLUE)
                               .build();
    dto.setAllowedScopeLevels(config.getAllowedScopeLevels());
    return dto;
  }

  public ResourceGroupConfig toConfig(ResourceGroupDTO dto) {
    return ResourceGroupConfig.builder()
        .identifier(dto.getIdentifier())
        .name(dto.getName())
        .fullScopeSelected(dto.isFullScopeSelected())
        .tags(dto.getTags())
        .resourceSelectors(dto.getResourceSelectors())
        .allowedScopeLevels(dto.getAllowedScopeLevels())
        .description(dto.getDescription())
        .build();
  }
}
