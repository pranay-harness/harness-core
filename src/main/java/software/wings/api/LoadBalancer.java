package software.wings.api;

import ro.fortsoft.pf4j.ExtensionPoint;

/**
 * Created by peeyushaggarwal on 10/3/16.
 */
public interface LoadBalancer<T extends LoadBalancerConfig> extends ExtensionPoint {
  boolean enableHost(T loadBalancerConfig, HostElement hostElement);

  boolean disableHost(T loadBalancerConfig, HostElement hostElement);

  Class<T> supportedConfig();
}
