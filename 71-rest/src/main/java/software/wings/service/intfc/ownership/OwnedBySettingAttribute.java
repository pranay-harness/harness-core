package software.wings.service.intfc.ownership;

public interface OwnedBySettingAttribute {
  /**
   * Prune if belongs to settingAttribute.
   *
   * @param appId the app id
   * @param settingId the setting id
   */
  void pruneBySettingAttribute(String appId, String settingId);
}
