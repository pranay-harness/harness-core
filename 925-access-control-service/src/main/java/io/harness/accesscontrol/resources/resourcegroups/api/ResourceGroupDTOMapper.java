/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.resources.resourcegroups.api;

import io.harness.accesscontrol.resourcegroups.api.ResourceGroupDTO;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class ResourceGroupDTOMapper {
  public static ResourceGroupDTO toDTO(ResourceGroup resourceGroup) {
    return ResourceGroupDTO.builder().identifier(resourceGroup.getIdentifier()).name(resourceGroup.getName()).build();
  }
}
