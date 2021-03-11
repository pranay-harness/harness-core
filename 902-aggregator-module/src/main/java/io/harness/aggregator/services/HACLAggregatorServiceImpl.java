package io.harness.aggregator.services;

import io.harness.accesscontrol.HUserPrincipal;
import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.daos.ACLDAO;
import io.harness.accesscontrol.acl.models.HACL;
import io.harness.accesscontrol.acl.models.HResource;
import io.harness.accesscontrol.acl.models.ParentMetadata;
import io.harness.accesscontrol.acl.models.SourceMetadata;
import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.aggregator.services.apis.ACLAggregatorService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class HACLAggregatorServiceImpl implements ACLAggregatorService {
  private final RoleAssignmentService roleAssignmentService;
  private final RoleService roleService;
  private final ResourceGroupClient resourceGroupClient;
  private final ACLDAO acldao;

  private Optional<ParentMetadata> getIdentifiersFromFQN(@NotEmpty String fqn) {
    fqn = fqn.replaceAll("^/", "");
    fqn = fqn.replaceAll("/$", "");
    String[] splitted = fqn.split("/");
    ParentMetadata.Builder builder = ParentMetadata.builder();
    if (splitted.length == 2) {
      builder.accountIdentifier(splitted[1]);
    } else if (splitted.length == 4) {
      builder.accountIdentifier(splitted[1]).orgIdentifier(splitted[3]);
    } else if (splitted.length == 6) {
      builder.accountIdentifier(splitted[1]).orgIdentifier(splitted[3]).projectIdentifier(splitted[5]);
    }
    ParentMetadata metadata = builder.build();
    return (Optional.ofNullable(metadata.getAccountIdentifier()).isPresent()) ? Optional.of(metadata)
                                                                              : Optional.empty();
  }

  @Override
  public boolean aggregate(Principal principal) {
    // first delete all ACLs for the user, and then repopulate
    acldao.deleteByPrincipal(principal);

    HUserPrincipal hPrincipal = (HUserPrincipal) principal;
    String principalIdentifier = hPrincipal.getPrincipalIdentifier();
    PrincipalType principalType = hPrincipal.getPrincipalType();

    RoleAssignmentFilter roleAssignmentFilter =
        RoleAssignmentFilter.builder()
            .principalFilter(Sets.newHashSet(io.harness.accesscontrol.principals.Principal.builder()
                                                 .principalIdentifier(principalIdentifier)
                                                 .principalType(principalType)
                                                 .build()))
            .build();
    int currentPageIndex = 0;
    PageResponse<RoleAssignment> currentPage;
    do {
      PageRequest currentPageRequest = PageRequest.builder().pageSize(100).pageIndex(currentPageIndex).build();
      currentPage = roleAssignmentService.list(currentPageRequest, roleAssignmentFilter);
      List<RoleAssignment> roleBindings = currentPage.getContent();
      for (RoleAssignment roleAssignment : roleBindings) {
        Optional<Role> role = roleService.get(
            roleAssignment.getRoleIdentifier(), roleAssignment.getScopeIdentifier(), ManagedFilter.NO_FILTER);
        if (role.isPresent()) {
          Set<String> permissions = role.get().getPermissions();
          String resourceIdentifier = roleAssignment.getResourceGroupIdentifier();
          Optional<ParentMetadata> parentMetadataOptional = getIdentifiersFromFQN(roleAssignment.getScopeIdentifier());
          if (parentMetadataOptional.isPresent()) {
            ParentMetadata parentMetadata = parentMetadataOptional.get();
            ResourceGroupDTO resourceGroup =
                NGRestUtils
                    .getResponse(
                        resourceGroupClient.getResourceGroup(resourceIdentifier, parentMetadata.getAccountIdentifier(),
                            parentMetadata.getOrgIdentifier(), parentMetadata.getProjectIdentifier()))
                    .getResourceGroup();
            permissions.forEach(permission
                -> resourceGroup.getResourceSelectors()
                       .stream()
                       .map(HResource::fromResourceSelector)
                       .flatMap(Collection::stream)
                       .filter(Optional::isPresent)
                       .map(Optional::get)
                       .forEach(hResource -> {
                         HACL acl = HACL.builder()
                                        .permission(permission)
                                        .resource(hResource)
                                        .sourceMetadata(SourceMetadata.builder()
                                                            .roleIdentifier(role.get().getIdentifier())
                                                            .userGroupIdentifier(null)
                                                            .roleAssignmentIdentifier(roleAssignment.getIdentifier())
                                                            .build())
                                        .resourceGroupIdentifier(resourceGroup.getIdentifier())
                                        .principal(hPrincipal)
                                        .aclQueryString(
                                            HACL.getAclQueryString(parentMetadata, hResource, hPrincipal, permission))
                                        .parentMetadata(parentMetadata)
                                        .build();
                         acldao.save(acl);
                       }));
          }
        }
      }
      currentPageIndex++;
    } while (currentPageIndex < currentPage.getTotalPages());
    return true;
  }
}
