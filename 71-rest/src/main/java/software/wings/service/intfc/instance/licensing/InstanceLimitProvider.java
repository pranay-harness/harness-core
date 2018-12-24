package software.wings.service.intfc.instance.licensing;

public interface InstanceLimitProvider {
  /**
   * Gets the maximum number of instances allowed for a given account based on their license.
   *
   * @param accountId account Id
   * @return max number of allowed instances
   */
  long getAllowedInstances(String accountId);
}