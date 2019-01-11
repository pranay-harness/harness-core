package software.wings.service.intfc.limits;

public interface LimitVicinityHandler {
  /**
   * Checks if a particular account is approaching limits consumption and takes appropriate actions (like raising
   * alerts) in that case
   */
  void checkAndAct(String accountId);
}
