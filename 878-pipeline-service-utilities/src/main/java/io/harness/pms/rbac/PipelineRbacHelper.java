package io.harness.pms.rbac;

import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.eraro.ErrorCode;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.WingsException;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class to perform validation checks on EntityDetail object. It constructs the runtimeAccess permission on its
 * own for the given referredEntity
 */
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineRbacHelper {
  @Inject EntityDetailProtoToRestMapper entityDetailProtoToRestMapper;
  @Inject @Named("PRIVILEGED") AccessControlClient accessControlClient;

  public void checkRuntimePermissions(Ambiance ambiance, Set<EntityDetailProtoDTO> entityDetailsProto) {
    List<EntityDetail> entityDetails =
        entityDetailProtoToRestMapper.createEntityDetailsDTO(new ArrayList<>(entityDetailsProto));
    ExecutionPrincipalInfo executionPrincipalInfo = ambiance.getMetadata().getPrincipalInfo();
    String principal = executionPrincipalInfo.getPrincipal();
    if (principal == null) {
      return;
    }
    PrincipalType principalType = PrincipalTypeProtoToPrincipalTypeMapper.convertToAccessControlPrincipalType(
        executionPrincipalInfo.getPrincipalType());
    List<PermissionCheckDTO> permissionCheckDTOS =
        entityDetails.stream().map(this::convertToPermissionCheckDTO).collect(Collectors.toList());
    AccessCheckResponseDTO accessCheckResponseDTO =
        accessControlClient.checkForAccess(principal, principalType, permissionCheckDTOS);
    if (accessCheckResponseDTO == null) {
      return;
    }
    List<AccessControlDTO> nonPermittedResources = accessCheckResponseDTO.getAccessControlList()
                                                       .stream()
                                                       .filter(accessControlDTO -> !accessControlDTO.isPermitted())
                                                       .collect(Collectors.toList());
    if (nonPermittedResources.size() != 0) {
      Map<String, List<String>> errors = new HashMap<>();
      for (AccessControlDTO accessControlDTO : nonPermittedResources) {
        List<String> resourceTypeErrors = errors.getOrDefault(accessControlDTO.getResourceType(), new ArrayList<>());
        resourceTypeErrors.add(accessControlDTO.getResourceIdentifier());
        errors.put(accessControlDTO.getResourceType(), resourceTypeErrors);
      }
      throw new AccessDeniedException(
          String.format("Access to the following resources missing: [%s]", errors.toString()),
          ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
  }

  public PermissionCheckDTO convertToPermissionCheckDTO(EntityDetail entityDetail) {
    IdentifierRef identifierRef = (IdentifierRef) entityDetail.getEntityRef();
    if (identifierRef.getMetadata() != null
        && identifierRef.getMetadata().getOrDefault("new", "false").equals("true")) {
      return PermissionCheckDTO.builder()
          .permission(PipelineReferredEntityPermissionHelper.getPermissionForGivenType(entityDetail.getType(), true))
          .resourceIdentifier(
              PipelineReferredEntityPermissionHelper.getParentResourceIdentifierForCreate(identifierRef))
          .resourceScope(PipelineReferredEntityPermissionHelper.getResourceScopeForCreate(identifierRef))
          .resourceType(PipelineReferredEntityPermissionHelper.getEntityTypeForCreate(identifierRef))
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

  public PermissionCheckDTO convertToPermissionCheckDTO(EntityDetailProtoDTO entityDetailProto) {
    EntityDetail entityDetail = entityDetailProtoToRestMapper.createEntityDetailDTO(entityDetailProto);
    return convertToPermissionCheckDTO(entityDetail);
  }
}
