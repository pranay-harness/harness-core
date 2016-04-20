package software.wings.security;

import software.wings.beans.User;

/**
 * Created by anubhaw on 4/20/16.
 */
public class UserThreadLocal {
  public static final ThreadLocal<User> userThreadLocal = new ThreadLocal<>();

  public static void set(User user) {
    userThreadLocal.set(user);
  }

  public static void unset() {
    userThreadLocal.remove();
  }

  public static User get() {
    return userThreadLocal.get();
  }
}
