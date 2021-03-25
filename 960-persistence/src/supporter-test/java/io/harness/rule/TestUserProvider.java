package io.harness.rule;

import io.harness.beans.EmbeddedUser;
import io.harness.persistence.UserProvider;

public class TestUserProvider implements UserProvider {
  public static final ThreadLocal<EmbeddedUser> userThreadLocal = new ThreadLocal<>();

  public static EmbeddedUser activeUser1 =
      EmbeddedUser.builder().uuid("uuid1").name("user1").email("test1@harness.io").build();
  public static EmbeddedUser activeUser2 =
      EmbeddedUser.builder().uuid("uuid2").name("user2").email("test2@harness.io").build();

  @Override
  public EmbeddedUser activeUser() {
    return userThreadLocal.get();
  }
}
