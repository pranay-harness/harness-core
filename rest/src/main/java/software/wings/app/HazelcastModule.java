package software.wings.app;

import com.google.inject.AbstractModule;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * Created by peeyushaggarwal on 1/11/17.
 */
public class HazelcastModule extends AbstractModule {
  private HazelcastInstance hazelcastInstance;

  public HazelcastModule() {
    Config config = new XmlConfigBuilder().build();
    config.setInstanceName(config.getConfigurationUrl().toString());
    this.hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(config);
  }

  public HazelcastModule(HazelcastInstance hazelcastInstance) {
    this.hazelcastInstance = hazelcastInstance;
  }

  @Override
  protected void configure() {
    bind(HazelcastInstance.class).toInstance(hazelcastInstance);
  }

  public HazelcastInstance getHazelcastInstance() {
    return hazelcastInstance;
  }
}
