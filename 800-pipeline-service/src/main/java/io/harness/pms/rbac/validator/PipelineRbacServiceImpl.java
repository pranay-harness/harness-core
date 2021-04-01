package io.harness.pms.rbac.validator;

import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.pipeline.PipelineSetupUsageHelper;
import io.harness.pms.rbac.PipelineRbacHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
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
  @Inject private PipelineRbacHelper pipelineRbacHelper;

  public void validateStaticallyReferredEntitiesInYaml(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String pipelineId, String pipelineYaml) {
    List<EntityDetail> entityDetails = pipelineSetupUsageHelper.getReferrencesOfPipeline(
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineId, pipelineYaml, null);
    List<PermissionCheckDTO> permissionCheckDTOS =
        entityDetails.stream().map(pipelineRbacHelper::convertToPermissionCheckDTO).collect(Collectors.toList());
    AccessCheckResponseDTO accessCheckResponseDTO = accessControlClient.checkForAccess(permissionCheckDTOS);
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

      throw new InvalidRequestException(
          String.format("Access to the following resources missing: [%s]", errors.toString()));
    }
  }
}
