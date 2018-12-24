package software.wings.service.intfc.instance.licensing;

public interface InstanceUsageLimitExcessHandler {
  /**
   * Checks whether given accounts instance usage is within limit.
   * And takes appropriate action otherwise.
   *
   * @param accountId
   */
  void handle(String accountId);
}