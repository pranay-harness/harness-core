package software.wings.app;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.hazelcast.core.HazelcastInstance;
import io.dropwizard.setup.Environment;
import io.harness.govern.DependencyModule;
import io.harness.govern.DependencyProviderModule;
import io.harness.hazelcast.HazelcastModule;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultMetaBroadcaster;
import org.atmosphere.cpr.MetaBroadcaster;
import software.wings.utils.HazelcastBroadcaster;

import java.util.Collections;
import java.util.Set;
import javax.servlet.ServletRegistration.Dynamic;

/**
 * Created by peeyushaggarwal on 8/16/16.
 */
public class StreamModule extends DependencyProviderModule {
  private Environment environment;

  private static void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
    HazelcastBroadcaster.HAZELCAST_INSTANCE.set(hazelcastInstance);
  }

  /**
   * Instantiates a new Push module.
   *
   * @param environment the environment
   */
  public StreamModule(Environment environment) {
    this.environment = environment;
  }

  @Provides
  @Singleton
  AtmosphereServlet getAtmosphereServelet(Provider<HazelcastInstance> hazelcastInstanceProvider) {
    AtmosphereServlet atmosphereServlet = new AtmosphereServlet();

    atmosphereServlet.framework()
        .addInitParameter(ApplicationConfig.WEBSOCKET_CONTENT_TYPE, "application/json")
        .addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true")
        .addInitParameter(ApplicationConfig.ANNOTATION_PACKAGE, UiStreamHandler.class.getPackage().getName());

    atmosphereServlet.framework().setDefaultBroadcasterClassName(HazelcastBroadcaster.class.getName());
    setHazelcastInstance(hazelcastInstanceProvider.get());
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
    Dynamic dynamic = environment.servlets().addServlet("StreamServlet", atmosphereServlet);
    dynamic.setAsyncSupported(true);
    dynamic.setLoadOnStartup(0);
    dynamic.addMapping("/stream/*");
    MetaBroadcaster metaBroadcaster = new DefaultMetaBroadcaster();
    metaBroadcaster.configure(atmosphereServlet.framework().getAtmosphereConfig());
    return metaBroadcaster;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return Collections.singleton(HazelcastModule.getInstance());
  }
}
