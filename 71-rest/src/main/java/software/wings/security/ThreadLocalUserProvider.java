package software.wings.security;

import io.harness.beans.EmbeddedUser;
import io.harness.persistence.UserProvider;
import software.wings.beans.User;

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
