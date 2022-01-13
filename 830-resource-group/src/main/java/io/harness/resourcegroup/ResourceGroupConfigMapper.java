/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
