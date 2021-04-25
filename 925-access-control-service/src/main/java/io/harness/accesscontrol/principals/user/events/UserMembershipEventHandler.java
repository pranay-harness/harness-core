package io.harness.accesscontrol.principals.user.events;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams.HarnessScopeParamsBuilder;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.usermembership.UserMembershipDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Collections;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@Slf4j
@Singleton
public class UserMembershipEventHandler implements EventHandler {
  private final RoleAssignmentService roleAssignmentService;
  private final ScopeService scopeService;

  @Inject
  public UserMembershipEventHandler(RoleAssignmentService roleAssignmentService, ScopeService scopeService) {
    this.roleAssignmentService = roleAssignmentService;
    this.scopeService = scopeService;
  }

  @Override
  public boolean handle(Message message) {
    UserMembershipDTO userMembershipDTO = null;
    try {
      userMembershipDTO = UserMembershipDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking EntityChangeDTO for user group event with key {}", message.getId(), e);
    }
    if (Objects.isNull(userMembershipDTO)) {
      return true;
    }
    try {
      String userId = userMembershipDTO.getUserId();
      io.harness.eventsframework.schemas.usermembership.Scope eventsScope = userMembershipDTO.getScope();

      HarnessScopeParamsBuilder builder =
          HarnessScopeParams.builder().accountIdentifier(stripToNull(eventsScope.getAccountIdentifier()));
      builder.orgIdentifier(stripToNull(eventsScope.getOrgIdentifier()));
      builder.projectIdentifier(stripToNull(eventsScope.getProjectIdentifier()));

      Scope scope = scopeService.buildScopeFromParams(builder.build());
      if (isNotEmpty(userId)) {
        deleteRoleAssignments(scope, userId);
      }
    } catch (Exception e) {
      log.error("Could not process the user membership delete event {} due to error", userMembershipDTO, e);
      return false;
    }
    return true;
  }

  private void deleteRoleAssignments(Scope scope, String principalIdentifier) {
    RoleAssignmentFilter roleAssignmentFilter =
        RoleAssignmentFilter.builder()
            .scopeFilter(scope.toString())
            .principalFilter(Collections.singleton(
                Principal.builder().principalIdentifier(principalIdentifier).principalType(PrincipalType.USER).build()))
            .build();
    roleAssignmentService.deleteMulti(roleAssignmentFilter);
  }
}
