/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.security.dto.PrincipalType.USER;

import io.harness.PipelineServiceConfiguration;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.user.UserInfo;
import io.harness.remote.client.RestClientUtils;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.UserPrincipal;
import io.harness.user.remote.UserClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

@Singleton
@OwnedBy(PIPELINE)
public class CurrentUserHelper {
  private static final EmbeddedUser DEFAULT_EMBEDDED_USER =
      EmbeddedUser.builder().uuid("lv0euRhKRCyiXWzS7pOg6g").name("Admin").email("admin@harness.io").build();

  @Inject private PipelineServiceConfiguration configuration;
  @Inject private UserClient userClient;

  public EmbeddedUser getFromSecurityContext() {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() == null
        || !USER.equals(SourcePrincipalContextBuilder.getSourcePrincipal().getType())) {
      throw new InvalidRequestException("Unable to fetch current user");
    }

    UserPrincipal userPrincipal = (UserPrincipal) SourcePrincipalContextBuilder.getSourcePrincipal();
    String userId = userPrincipal.getName();
    Optional<UserInfo> userOptional = RestClientUtils.getResponse(userClient.getUserById(userId));
    if (!userOptional.isPresent()) {
      throw new InvalidRequestException(String.format("Invalid user: %s", userId));
    }
    UserInfo user = userOptional.get();
    return EmbeddedUser.builder().uuid(user.getUuid()).name(user.getName()).email(user.getEmail()).build();
  }
}
