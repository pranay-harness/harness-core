package io.harness.pms.rbac.validator;

import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.pipeline.PipelineSetupUsageHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class PipelineRbacServiceImpl implements PipelineRbacService {
  @Inject private PipelineSetupUsageHelper pipelineSetupUsageHelper;
  @Inject private AccessControlClient accessControlClient;

  public void validateStaticallyReferredEntitiesInYaml(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String pipelineId, String pipelineYaml) {
    List<EntityDetail> entityDetails = pipelineSetupUsageHelper.getReferrencesOfPipeline(
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineId, pipelineYaml, null);
    List<PermissionCheckDTO> permissionCheckDTOS =
        entityDetails.stream().map(this::convertToPermissionCheckDTO).collect(Collectors.toList());
    AccessCheckResponseDTO accessCheckResponseDTO = accessControlClient.checkForAccess(permissionCheckDTOS);
    if (accessCheckResponseDTO == null) {
      return;
    }
    List<AccessControlDTO> nonPermittedResources = accessCheckResponseDTO.getAccessControlList()
                                                       .stream()
                                                       .filter(accessControlDTO -> !accessControlDTO.isPermitted())
                                                       .collect(Collectors.toList());
    if (nonPermittedResources.size() != 0) {
      Map<String, String> errors = nonPermittedResources.stream().collect(
          Collectors.toMap(AccessControlDTO::getResourceType, AccessControlDTO::getResourceIdentifier));
      throw new InvalidRequestException(
          String.format("Access to the following resources missing: [%s]", errors.toString()));
    }
  }

  private PermissionCheckDTO convertToPermissionCheckDTO(EntityDetail entityDetail) {
    IdentifierRef identifierRef = (IdentifierRef) entityDetail.getEntityRef();
    if (identifierRef.getMetadata() != null
        && identifierRef.getMetadata().getOrDefault("new", "false").equals("true")) {
      return PermissionCheckDTO.builder()
          .permission(PipelineReferredEntityPermissionHelper.getPermissionForGivenType(entityDetail.getType(), true))
          .resourceIdentifier(identifierRef.getIdentifier())
          .resourceScope(ResourceScope.builder()
                             .accountIdentifier(identifierRef.getAccountIdentifier())
                             .orgIdentifier(identifierRef.getOrgIdentifier())
                             .projectIdentifier(identifierRef.getProjectIdentifier())
                             .build())
          .resourceType(PipelineReferredEntityPermissionHelper.getEntityName(entityDetail.getType()))
          .build();
    }
    return PermissionCheckDTO.builder()
        .permission(PipelineReferredEntityPermissionHelper.getPermissionForGivenType(entityDetail.getType(), false))
        .resourceIdentifier(identifierRef.getIdentifier())
        .resourceScope(ResourceScope.builder()
                           .accountIdentifier(identifierRef.getAccountIdentifier())
                           .orgIdentifier(identifierRef.getOrgIdentifier())
                           .projectIdentifier(identifierRef.getProjectIdentifier())
                           .build())
        .resourceType(PipelineReferredEntityPermissionHelper.getEntityName(entityDetail.getType()))
        .build();
  }
}
