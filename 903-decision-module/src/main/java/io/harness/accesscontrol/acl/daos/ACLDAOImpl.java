package io.harness.accesscontrol.acl.daos;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.HACL;
import io.harness.accesscontrol.acl.models.HResource;
import io.harness.accesscontrol.acl.models.ParentMetadata;
import io.harness.accesscontrol.acl.repository.ACLRepository;
import io.harness.accesscontrol.clients.PermissionCheckDTO;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class ACLDAOImpl implements ACLDAO {
  public static final String ACCOUNT = "ACCOUNT";
  public static final String ORG = "ORG";
  public static final String PROJECT = "PROJECT";
  private final ACLRepository aclRepository;

  private String getACLQuery(
      ParentMetadata parentMetadata, HResource resource, Principal hPrincipal, String permission) {
    return HACL.getAclQueryString(parentMetadata, resource, hPrincipal, permission);
  }

  private List<ACL> processACLQueries(List<PermissionCheckDTO> permissionsRequired, Principal hPrincipal) {
    List<ACL> aclList = new ArrayList<>();
    permissionsRequired.forEach(permissionCheckDTO -> {
      ParentMetadata parentMetadata =
          ParentMetadata.builder()
              .accountIdentifier(permissionCheckDTO.getResourceScope().getAccountIdentifier())
              .orgIdentifier(permissionCheckDTO.getResourceScope().getOrgIdentifier())
              .projectIdentifier(permissionCheckDTO.getResourceScope().getProjectIdentifier())
              .build();

      Optional<String> queryStringForAccountScope =
          Optional.ofNullable(parentMetadata.getAccountIdentifier())
              .map(x
                  -> getACLQuery(parentMetadata,
                      HResource.builder().resourceIdentifier(x).resourceType(ACCOUNT).build(), hPrincipal,
                      permissionCheckDTO.getPermission()));

      Optional<String> queryStringForOrgScope =
          Optional.ofNullable(parentMetadata.getOrgIdentifier())
              .map(x
                  -> getACLQuery(parentMetadata, HResource.builder().resourceIdentifier(x).resourceType(ORG).build(),
                      hPrincipal, permissionCheckDTO.getPermission()));

      Optional<String> queryStringForProjectScope =
          Optional.ofNullable(parentMetadata.getProjectIdentifier())
              .map(x
                  -> getACLQuery(parentMetadata,
                      HResource.builder().resourceIdentifier(x).resourceType(PROJECT).build(), hPrincipal,
                      permissionCheckDTO.getPermission()));

      Optional<String> queryStringForResourceIdentifier =
          Optional.ofNullable(permissionCheckDTO.getResourceIdentifier())
              .map(x
                  -> getACLQuery(parentMetadata,
                      HResource.builder()
                          .resourceIdentifier(x)
                          .resourceType(permissionCheckDTO.getResourceType())
                          .build(),
                      hPrincipal, permissionCheckDTO.getPermission()));

      String queryStringForAllResources = getACLQuery(parentMetadata,
          HResource.builder()
              .resourceIdentifier(HResource.IDENTIFIER_FOR_ALL_RESOURCES)
              .resourceType(permissionCheckDTO.getResourceType())
              .build(),
          hPrincipal, permissionCheckDTO.getPermission());

      List<String> aclQueryStrings = Lists.newArrayList(queryStringForAllResources);
      queryStringForResourceIdentifier.ifPresent(aclQueryStrings::add);
      queryStringForAccountScope.ifPresent(aclQueryStrings::add);
      queryStringForOrgScope.ifPresent(aclQueryStrings::add);
      queryStringForProjectScope.ifPresent(aclQueryStrings::add);
      List<ACL> aclsInDB = aclRepository.getByAclQueryStringIn(aclQueryStrings);

      if (!aclsInDB.isEmpty()) {
        aclList.add(aclsInDB.get(0));
      } else {
        aclList.add(null);
      }
    });
    return aclList;
  }

  @Override
  public List<ACL> get(Principal principal, List<PermissionCheckDTO> permissionsRequired) {
    return processACLQueries(permissionsRequired, principal);
  }

  @Override
  public ACL save(ACL acl) {
    return aclRepository.save(acl);
  }

  @Override
  public void deleteByPrincipal(String principalType, String principalIdentifier) {
    aclRepository.deleteByPrincipalTypeAndPrincipalIdentifier(principalType, principalIdentifier);
  }

  @Override
  public List<ACL> saveAll(List<ACL> acls) {
    return StreamSupport.stream(aclRepository.saveAll(acls).spliterator(), false).collect(Collectors.toList());
  }

  @Override
  public void deleteAll(List<ACL> acls) {
    aclRepository.deleteAll(acls);
  }

  @Override
  public void deleteByRoleAssignmentId(String roleAssignmentId) {
    aclRepository.deleteByRoleAssignmentId(roleAssignmentId);
  }

  @Override
  public List<ACL> getByRole(String scopeIdentifier, String identifier, boolean managed) {
    return aclRepository.findByRole(scopeIdentifier, identifier, managed);
  }

  @Override
  public List<ACL> getByResourceGroup(String scopeIdentifier, String identifier, boolean managed) {
    return aclRepository.findByResourceGroup(scopeIdentifier, identifier, managed);
  }
}
