/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.rbac.validator;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.pipeline.PipelineSetupUsageHelper;
import io.harness.pms.rbac.PipelineRbacHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class PipelineRbacServiceImpl implements PipelineRbacService {
  @Inject private PipelineSetupUsageHelper pipelineSetupUsageHelper;
  @Inject private AccessControlClient accessControlClient;
  @Inject private PipelineRbacHelper pipelineRbacHelper;

  public void extractAndValidateStaticallyReferredEntities(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String pipelineId, String pipelineYaml) {
    List<EntityDetail> entityDetails = pipelineSetupUsageHelper.getReferencesOfPipeline(
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineId, pipelineYaml, null);
    validateStaticallyReferredEntities(
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineId, pipelineYaml, entityDetails);
  }

  public void validateStaticallyReferredEntities(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String pipelineId, String pipelineYaml, List<EntityDetail> entityDetails) {
    List<PermissionCheckDTO> permissionCheckDTOS =
        entityDetails.stream().map(pipelineRbacHelper::convertToPermissionCheckDTO).collect(Collectors.toList());
    if (isNotEmpty(permissionCheckDTOS)) {
      AccessCheckResponseDTO accessCheckResponseDTO = accessControlClient.checkForAccess(permissionCheckDTOS);
      if (accessCheckResponseDTO == null) {
        return;
      }
      List<AccessControlDTO> nonPermittedResources = accessCheckResponseDTO.getAccessControlList()
                                                         .stream()
                                                         .filter(accessControlDTO -> !accessControlDTO.isPermitted())
                                                         .collect(Collectors.toList());
      if (nonPermittedResources.size() != 0) {
        PipelineRbacHelper.throwAccessDeniedError(nonPermittedResources);
      }
    }
  }
}
