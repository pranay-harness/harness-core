package software.wings.beans.infrastructure;

import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 10/4/16.
 */
public abstract class InfrastructureProviderConfig extends SettingValue {
  /**
   * Instantiates a new setting value.
   *
   * @param type the type
   */
  public InfrastructureProviderConfig(String type) {
    super(type);
  }
}
