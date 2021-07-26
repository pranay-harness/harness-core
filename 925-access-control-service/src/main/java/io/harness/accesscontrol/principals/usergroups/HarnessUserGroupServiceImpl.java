package io.harness.accesscontrol.principals.usergroups;

import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ACCOUNT_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ORG_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.PROJECT_LEVEL_PARAM_NAME;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeParams;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.remote.client.NGRestUtils;
import io.harness.usergroups.UserGroupClient;
import io.harness.utils.RetryUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@OwnedBy(PL)
@Singleton
@ValidateOnExecution
@Slf4j
public class HarnessUserGroupServiceImpl implements HarnessUserGroupService {
  private final UserGroupClient userGroupClient;
  private final UserGroupFactory userGroupFactory;
  private final UserGroupService userGroupService;
  private final ScopeParamsFactory scopeParamsFactory;

  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("Could not find the user group with the given identifier on attempt %s",
          "Could not find the user group with the given identifier", Lists.newArrayList(InvalidRequestException.class),
          Duration.ofSeconds(5), 3, log);

  @Inject
  public HarnessUserGroupServiceImpl(@Named("PRIVILEGED") UserGroupClient userGroupClient,
      UserGroupFactory userGroupFactory, UserGroupService userGroupService, ScopeParamsFactory scopeParamsFactory) {
    this.userGroupClient = userGroupClient;
    this.userGroupFactory = userGroupFactory;
    this.userGroupService = userGroupService;
    this.scopeParamsFactory = scopeParamsFactory;
  }

  @Override
  public void sync(String identifier, Scope scope) {
    ScopeParams scopeParams = scopeParamsFactory.buildScopeParams(scope);
    try {
      Optional<UserGroupDTO> userGroupDTOOpt =
          Optional.ofNullable(Failsafe.with(retryPolicy)
                                  .get(()
                                           -> NGRestUtils.getResponse(userGroupClient.getUserGroup(identifier,
                                               scopeParams.getParams().get(ACCOUNT_LEVEL_PARAM_NAME),
                                               scopeParams.getParams().get(ORG_LEVEL_PARAM_NAME),
                                               scopeParams.getParams().get(PROJECT_LEVEL_PARAM_NAME)))));
      if (userGroupDTOOpt.isPresent()) {
        userGroupService.upsert(userGroupFactory.buildUserGroup(userGroupDTOOpt.get()));
      } else {
        deleteIfPresent(identifier, scope);
      }
    } catch (Exception e) {
      log.error("Exception while syncing user groups", e);
    }
  }

  public void deleteIfPresent(String identifier, Scope scope) {
    log.warn("Removing user group with identifier {} in scope {}.", identifier, scope.toString());
    userGroupService.deleteIfPresent(identifier, scope.toString());
  }
}
