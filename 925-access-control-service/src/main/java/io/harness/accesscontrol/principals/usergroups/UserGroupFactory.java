package io.harness.accesscontrol.principals.usergroups;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.UserGroupDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@OwnedBy(PL)
@Singleton
public class UserGroupFactory {
  private final ScopeService scopeService;

  @Inject
  public UserGroupFactory(ScopeService scopeService) {
    this.scopeService = scopeService;
  }

  public UserGroup buildUserGroup(UserGroupDTO userGroupDTO) {
    HarnessScopeParams harnessScopeParams = HarnessScopeParams.builder()
                                                .accountIdentifier(userGroupDTO.getAccountIdentifier())
                                                .orgIdentifier(userGroupDTO.getOrgIdentifier())
                                                .projectIdentifier(userGroupDTO.getProjectIdentifier())
                                                .build();
    Scope scope = scopeService.buildScopeFromParams(harnessScopeParams);
    Set<String> users =
        userGroupDTO.getUsers() == null ? Collections.emptySet() : new HashSet<>(userGroupDTO.getUsers());
    return UserGroup.builder()
        .identifier(userGroupDTO.getIdentifier())
        .scopeIdentifier(scope.toString())
        .name(userGroupDTO.getName())
        .users(users)
        .build();
  }
}
