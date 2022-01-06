/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.principals.users;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.remote.client.NGRestUtils;
import io.harness.usermembership.remote.UserMembershipClient;
import io.harness.utils.RetryUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class HarnessUserServiceImpl implements HarnessUserService {
  private final UserMembershipClient userMembershipClient;
  private final UserService userService;

  @Inject
  public HarnessUserServiceImpl(
      @Named("PRIVILEGED") UserMembershipClient userMembershipClient, UserService userService) {
    this.userMembershipClient = userMembershipClient;
    this.userService = userService;
  }

  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("Could not find the user with the given identifier on attempt %s",
          "Could not find the user with the given identifier", Lists.newArrayList(InvalidRequestException.class),
          Duration.ofSeconds(5), 3, log);

  @Override
  public void sync(String identifier, Scope scope) {
    HarnessScopeParams scopeParams = ScopeMapper.toParams(scope);
    Boolean isUserInScope = Failsafe.with(retryPolicy)
                                .get(()
                                         -> NGRestUtils.getResponse(userMembershipClient.isUserInScope(identifier,
                                             scopeParams.getAccountIdentifier(), scopeParams.getOrgIdentifier(),
                                             scopeParams.getProjectIdentifier())));
    if (Boolean.TRUE.equals(isUserInScope)) {
      User user = User.builder().identifier(identifier).scopeIdentifier(scope.toString()).build();
      userService.createIfNotPresent(user);
    } else {
      userService.deleteIfPresent(identifier, scope.toString());
    }
  }
}
