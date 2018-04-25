package software.wings.service.intfc;

import software.wings.settings.UsageRestrictions;

/**
 *
 * @author rktummala on 03/04/18
 */
public interface UsageRestrictionService {
  /**
   *
   * @param accountId
   * @param settingAttributeId
   * @return
   */
  UsageRestrictions getUsageRestrictions(String accountId, String settingAttributeId);

  /**
   *
   * @param accountId
   * @param settingAttributeId
   * @param usageRestrictions
   * @return
   */
  UsageRestrictions updateUsageRestrictions(
      String accountId, String settingAttributeId, UsageRestrictions usageRestrictions);
}
