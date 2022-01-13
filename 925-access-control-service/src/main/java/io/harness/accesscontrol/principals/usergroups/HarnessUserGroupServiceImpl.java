/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.principals.usergroups;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
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
  private final UserGroupService userGroupService;

  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("Could not find the user group with the given identifier on attempt %s",
          "Could not find the user group with the given identifier", Lists.newArrayList(InvalidRequestException.class),
          Duration.ofSeconds(5), 3, log);

  @Inject
  public HarnessUserGroupServiceImpl(
      @Named("PRIVILEGED") UserGroupClient userGroupClient, UserGroupService userGroupService) {
    this.userGroupClient = userGroupClient;
    this.userGroupService = userGroupService;
  }

  @Override
  public void sync(String identifier, Scope scope) {
    HarnessScopeParams scopeParams = ScopeMapper.toParams(scope);
    try {
      Optional<UserGroupDTO> userGroupDTOOpt =
          Optional.ofNullable(Failsafe.with(retryPolicy)
                                  .get(()
                                           -> NGRestUtils.getResponse(userGroupClient.getUserGroup(identifier,
                                               scopeParams.getAccountIdentifier(), scopeParams.getOrgIdentifier(),
                                               scopeParams.getProjectIdentifier()))));
      if (userGroupDTOOpt.isPresent()) {
        userGroupService.upsert(UserGroupFactory.buildUserGroup(userGroupDTOOpt.get()));
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
