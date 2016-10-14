package software.wings.security;

import software.wings.beans.User;

/**
 * Created by anubhaw on 4/20/16.
 */
public class UserThreadLocal {
  /**
   * The constant userThreadLocal.
   */
  public static final ThreadLocal<User> userThreadLocal = new ThreadLocal<>();

  /**
   * Sets the.
   *
   * @param user the user
   */
  public static void set(User user) {
    userThreadLocal.set(user);
  }

  /**
   * Unset.
   */
  public static void unset() {
    userThreadLocal.remove();
  }

  /**
   * Gets the.
   *
   * @return the user
   */
  public static User get() {
    return userThreadLocal.get();
  }
}
