package io.harness.stream;

import static io.harness.stream.AtmosphereBroadcaster.HAZELCAST;
import static io.harness.stream.AtmosphereBroadcaster.REDIS;
import static io.harness.stream.redisson.RedissonFactory.setInitParameters;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.hazelcast.core.HazelcastInstance;
import io.harness.govern.DependencyModule;
import io.harness.govern.DependencyProviderModule;
import io.harness.hazelcast.HazelcastModule;
import io.harness.redis.RedisConfig;
import io.harness.stream.hazelcast.HazelcastBroadcaster;
import io.harness.stream.redisson.RedissonBroadcaster;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.atmosphere.cpr.DefaultMetaBroadcaster;
import org.atmosphere.cpr.MetaBroadcaster;

import java.util.Collections;
import java.util.Set;

public class StreamModule extends DependencyProviderModule {
  private AtmosphereBroadcaster atmosphereBroadcaster;

  public StreamModule(AtmosphereBroadcaster atmosphereBroadcaster) {
    this.atmosphereBroadcaster = atmosphereBroadcaster;
  }

  @Provides
  @Singleton
  AtmosphereServlet getAtmosphereServelet(Provider<HazelcastInstance> hazelcastInstanceProvider,
      @Named("atmosphere") Provider<RedisConfig> redisConfigProvider) {
    AtmosphereServlet atmosphereServlet = new AtmosphereServlet();
    atmosphereServlet.framework()
        .addInitParameter(ApplicationConfig.WEBSOCKET_CONTENT_TYPE, "application/json")
        .addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true")
        .addInitParameter(ApplicationConfig.ANNOTATION_PACKAGE, getClass().getPackage().getName());

    String broadcasterName;
    if (atmosphereBroadcaster == HAZELCAST) {
      broadcasterName = HazelcastBroadcaster.class.getName();
      HazelcastBroadcaster.HAZELCAST_INSTANCE.set(hazelcastInstanceProvider.get());
    } else if (atmosphereBroadcaster == REDIS) {
      broadcasterName = RedissonBroadcaster.class.getName();
      setInitParameters(atmosphereServlet, redisConfigProvider.get());
    } else {
      broadcasterName = DefaultBroadcaster.class.getName();
    }
    atmosphereServlet.framework().setDefaultBroadcasterClassName(broadcasterName);
    return atmosphereServlet;
  }

  @Provides
  @Singleton
  BroadcasterFactory getBroadcasterFactory(AtmosphereServlet atmosphereServlet) {
    return atmosphereServlet.framework().getBroadcasterFactory();
  }

  @Provides
  @Singleton
  MetaBroadcaster metaBroadcaster(AtmosphereServlet atmosphereServlet) {
    MetaBroadcaster metaBroadcaster = new DefaultMetaBroadcaster();
    metaBroadcaster.configure(atmosphereServlet.framework().getAtmosphereConfig());
    return metaBroadcaster;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return Collections.singleton(HazelcastModule.getInstance());
  }
}
