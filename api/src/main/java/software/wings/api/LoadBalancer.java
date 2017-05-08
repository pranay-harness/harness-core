package software.wings.api;

import ro.fortsoft.pf4j.ExtensionPoint;
import software.wings.sm.ContextElement;

/**
 * Created by peeyushaggarwal on 10/3/16.
 */
public interface LoadBalancer<T extends LoadBalancerConfig> extends ExtensionPoint {
  boolean enableInstance(T loadBalancerConfig, ContextElement hostElement);

  boolean disableInstance(T loadBalancerConfig, ContextElement hostElement);

  Class<T> supportedConfig();
}
