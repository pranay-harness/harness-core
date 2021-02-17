package io.harness.accesscontrol.acl.daos;

import io.harness.accesscontrol.HPrincipal;
import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.dtos.PermissionCheckDTO;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.HACL;
import io.harness.accesscontrol.acl.models.HResource;
import io.harness.accesscontrol.acl.models.ParentMetadata;
import io.harness.accesscontrol.acl.repository.HACLRepository;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class HACLDAOImpl implements ACLDAO {
  private final HACLRepository accessControlRepository;

  private String getACLQuery(
      ParentMetadata parentMetadata, HResource resource, HPrincipal hPrincipal, String permission) {
    return HACL.getAclQueryString(parentMetadata, resource, hPrincipal, permission);
  }

  @Override
  public List<ACL> get(Principal principal, List<PermissionCheckDTO> permissionsRequired) {
    List<ACL> aclList = new ArrayList<>();
    HPrincipal hPrincipal = (HPrincipal) principal;
    permissionsRequired.forEach(permissionCheckDTO -> {
      ParentMetadata parentMetadata = ParentMetadata.builder()
                                          .accountIdentifier(permissionCheckDTO.getAccountIdentifier())
                                          .orgIdentifier(permissionCheckDTO.getOrgIdentifier())
                                          .projectIdentifier(permissionCheckDTO.getProjectIdentifier())
                                          .build();
      String queryStringForResourceIdentifier = getACLQuery(parentMetadata,
          HResource.builder()
              .resourceIdentifier(permissionCheckDTO.getResourceIdentifier())
              .resourceType(permissionCheckDTO.getResourceType())
              .build(),
          hPrincipal, permissionCheckDTO.getPermission());
      String queryStringForAllResources = getACLQuery(parentMetadata,
          HResource.builder().resourceIdentifier(null).resourceType(permissionCheckDTO.getResourceType()).build(),
          hPrincipal, permissionCheckDTO.getPermission());
      List<ACL> aclsInDB = accessControlRepository.getByAclQueryStringIn(
          Lists.newArrayList(queryStringForResourceIdentifier, queryStringForAllResources));
      if (!aclsInDB.isEmpty()) {
        aclList.add(aclsInDB.get(0));
      } else {
        aclList.add(null);
      }
    });
    return aclList;
  }

  @Override
  public ACL save(ACL acl) {
    return accessControlRepository.save((HACL) acl);
  }
}
