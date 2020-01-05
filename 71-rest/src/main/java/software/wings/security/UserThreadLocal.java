package software.wings.security;

import lombok.experimental.UtilityClass;
import software.wings.beans.User;

@UtilityClass
public class UserThreadLocal {
  public static class Guard implements AutoCloseable {
    private User old;
    Guard(User user) {
      old = get();
      set(user);
    }

    @Override
    public void close() {
      set(old);
    }
  }

  public static Guard userGuard(User user) {
    return new Guard(user);
  }

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
