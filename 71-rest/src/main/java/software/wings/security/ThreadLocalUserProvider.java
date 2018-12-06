package software.wings.security;

import io.harness.beans.EmbeddedUser;
import io.harness.persistence.UserProvider;
import software.wings.beans.User;

public class ThreadLocalUserProvider implements UserProvider {
  @Override
  public EmbeddedUser activeUser() {
    User user = UserThreadLocal.get();
    if (user == null) {
      return null;
    }

    return EmbeddedUser.builder()
        .uuid(UserThreadLocal.get().getUuid())
        .email(UserThreadLocal.get().getEmail())
        .name(UserThreadLocal.get().getName())
        .build();
  }
}
