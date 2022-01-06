/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.UserProvider;

import software.wings.beans.User;

@OwnedBy(PL)
public class ThreadLocalUserProvider implements UserProvider {
  public static EmbeddedUser populateEmbeddedUser(User user) {
    return EmbeddedUser.builder()
        .uuid(UserThreadLocal.get().getUuid())
        .email(UserThreadLocal.get().getEmail())
        .name(UserThreadLocal.get().getName())
        .build();
  }

  public static EmbeddedUser threadLocalUser() {
    User user = UserThreadLocal.get();
    if (user == null) {
      return null;
    }

    return populateEmbeddedUser(UserThreadLocal.get());
  }

  @Override
  public EmbeddedUser activeUser() {
    return threadLocalUser();
  }
}
