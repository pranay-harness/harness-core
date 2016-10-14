package software.wings.api;

import software.wings.beans.SettingValue;

/**
 * Created by peeyushaggarwal on 9/15/16.
 */
public abstract class LoadBalancerConfig extends SettingValue {
  /**
   * Instantiates a new setting value.
   *
   * @param type the type
   */
  public LoadBalancerConfig(SettingVariableTypes type) {
    super(type);
  }
}
