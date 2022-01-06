/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.helpers;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.UserPrincipal;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class ScmUserHelper {
  public static EmbeddedUser getCurrentUser() {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      UserPrincipal userPrincipal = (UserPrincipal) SourcePrincipalContextBuilder.getSourcePrincipal();
      return EmbeddedUser.builder()
          .uuid(userPrincipal.getName())
          .name(userPrincipal.getUsername())
          .email(userPrincipal.getEmail())
          .build();
    }
    return EmbeddedUser.builder()
        .email(GitSyncConstants.DEFAULT_USER_EMAIL_ID)
        .name(GitSyncConstants.DEFAULT_USER_NAME)
        .build();
  }
}
