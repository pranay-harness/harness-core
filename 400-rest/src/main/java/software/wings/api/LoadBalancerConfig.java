package software.wings.api;

import static software.wings.audit.ResourceType.LOAD_BALANCER;

import software.wings.settings.SettingValue;

/**
 * Created by peeyushaggarwal on 9/15/16.
 */
public abstract class LoadBalancerConfig extends SettingValue {
  /**
   * Instantiates a new setting value.
   *
   * @param type the type
   */
  public LoadBalancerConfig(String type) {
    super(type);
  }

  @Override
  public String fetchResourceCategory() {
    return LOAD_BALANCER.name();
  }
}
