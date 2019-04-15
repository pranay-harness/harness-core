package software.wings.service.intfc.instance.licensing;

public interface InstanceUsageLimitChecker {
  /**
   * Tells whether the instance usage of a given account is within the limits of license or not.
   * If
   *
   *   maxAllowedUsage = L (for limit), usage = U
   *
   * then this method will return true if
   *
   *    U < (percentLimit/100.0) * L
   *
   * @param accountId account Id for which to check limits
   * @return whether usage is within limits or not
   */
  boolean isWithinLimit(String accountId, long percentLimit, double actualUsage);
}