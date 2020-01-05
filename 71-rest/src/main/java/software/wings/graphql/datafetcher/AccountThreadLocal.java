package software.wings.graphql.datafetcher;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AccountThreadLocal {
  public static final ThreadLocal<String> accountIdThreadLocal = new ThreadLocal<>();

  /**
   * Sets the.
   *
   * @param user the user
   */
  public static void set(String accountId) {
    accountIdThreadLocal.set(accountId);
  }

  /**
   * Unset.
   */
  public static void unset() {
    accountIdThreadLocal.remove();
  }

  /**
   * Gets the.
   *
   * @return the user
   */
  public static String get() {
    return accountIdThreadLocal.get();
  }
}
