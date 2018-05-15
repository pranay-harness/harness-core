package software.wings.app;

import com.google.inject.AbstractModule;

import io.dropwizard.setup.Environment;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultMetaBroadcaster;
import org.atmosphere.cpr.MetaBroadcaster;
import software.wings.service.impl.EventEmitter;

import javax.servlet.ServletRegistration.Dynamic;

/**
 * Created by peeyushaggarwal on 8/16/16.
 */
public class StreamModule extends AbstractModule {
  private AtmosphereServlet atmosphereServlet;
  private MetaBroadcaster metaBroadcaster;

  /**
   * Instantiates a new Push module.
   *
   * @param environment the environment
   */
  public StreamModule(Environment environment) {
    atmosphereServlet = new AtmosphereServlet();

    atmosphereServlet.framework()
        .addInitParameter(ApplicationConfig.WEBSOCKET_CONTENT_TYPE, "application/json")
        .addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true")
        .addInitParameter(ApplicationConfig.ANNOTATION_PACKAGE, UiStreamHandler.class.getPackage().getName());

    Dynamic dynamic = environment.servlets().addServlet("StreamServlet", atmosphereServlet);
    dynamic.setAsyncSupported(true);
    dynamic.setLoadOnStartup(0);
    dynamic.addMapping("/stream/*");
    metaBroadcaster = new DefaultMetaBroadcaster();
    metaBroadcaster.configure(atmosphereServlet.framework().getAtmosphereConfig());
  }

  @Override
  protected void configure() {
    bind(EventEmitter.class);
    bind(BroadcasterFactory.class).toInstance(atmosphereServlet.framework().getBroadcasterFactory());
    bind(MetaBroadcaster.class).toInstance(metaBroadcaster);
  }

  /**
   * Gets atmosphere servlet.
   *
   * @return the atmosphere servlet
   */
  public AtmosphereServlet getAtmosphereServlet() {
    return atmosphereServlet;
  }
}
