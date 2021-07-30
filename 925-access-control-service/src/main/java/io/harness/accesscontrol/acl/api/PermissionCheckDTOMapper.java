package io.harness.accesscontrol.acl.api;

import io.harness.accesscontrol.acl.PermissionCheck;
import io.harness.accesscontrol.acl.PermissionCheckResult;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@Singleton
@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class PermissionCheckDTOMapper {
  static PermissionCheck fromDTO(PermissionCheckDTO permissionCheckDTO) {
    return PermissionCheck.builder()
        .permission(permissionCheckDTO.getPermission())
        .resourceIdentifier(permissionCheckDTO.getResourceIdentifier())
        .resourceType(permissionCheckDTO.getResourceType())
        .resourceScope(getScope(permissionCheckDTO.getResourceScope()).orElse(null))
        .build();
  }

  static AccessControlDTO toDTO(PermissionCheckResult permissionCheckResult) {
    return AccessControlDTO.builder()
        .permission(permissionCheckResult.getPermission())
        .resourceIdentifier(permissionCheckResult.getResourceIdentifier())
        .resourceType(permissionCheckResult.getResourceType())
        .resourceScope(getResourceScope(permissionCheckResult.getResourceScope()).orElse(null))
        .permitted(permissionCheckResult.isPermitted())
        .build();
  }

  private Optional<Scope> getScope(ResourceScope resourceScope) {
    if (resourceScope != null && !StringUtils.isEmpty(resourceScope.getAccountIdentifier())) {
      return Optional.of(ScopeMapper.fromParams(HarnessScopeParams.builder()
                                                    .accountIdentifier(resourceScope.getAccountIdentifier())
                                                    .orgIdentifier(resourceScope.getOrgIdentifier())
                                                    .projectIdentifier(resourceScope.getProjectIdentifier())
                                                    .build()));
    }
    return Optional.empty();
  }

  private Optional<ResourceScope> getResourceScope(Scope scope) {
    if (scope != null) {
      HarnessScopeParams harnessScopeParams = ScopeMapper.toParams(scope);
      return Optional.of(ResourceScope.builder()
                             .accountIdentifier(harnessScopeParams.getAccountIdentifier())
                             .orgIdentifier(harnessScopeParams.getOrgIdentifier())
                             .projectIdentifier(harnessScopeParams.getOrgIdentifier())
                             .build());
    }
    return Optional.empty();
  }
}
