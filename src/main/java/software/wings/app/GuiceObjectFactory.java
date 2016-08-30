package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereObjectFactory;
import org.atmosphere.cpr.AtmosphereResourceFactory;
import org.atmosphere.cpr.AtmosphereResourceSessionFactory;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.MetaBroadcaster;
import org.atmosphere.inject.AtmosphereProducers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peeyushaggarwal on 8/18/16.
 */
public class GuiceObjectFactory implements AtmosphereObjectFactory<AbstractModule> {
  private static final Logger logger = LoggerFactory.getLogger(GuiceObjectFactory.class);
  private final List<AbstractModule> modules = new ArrayList<AbstractModule>();
  /**
   * The Injector.
   */
  protected Injector injector;
  /**
   * The Config.
   */
  protected AtmosphereConfig config;

  /**
   * Instantiates a new Guice object factory.
   *
   * @param injector the injector
   */
  public GuiceObjectFactory(Injector injector) {
    this.injector = injector;
  }

  @Override
  public void configure(AtmosphereConfig config) {
    if (config == null) {
      throw new NullPointerException("AtmosphereConfig can't be null");
    }
    this.config = config;

    modules.add(new AtmosphereModule());

    try {
      AtmosphereProducers p = newClassInstance(AtmosphereProducers.class, AtmosphereProducers.class);
      p.configure(config);
    } catch (Exception e) {
      logger.error("", e);
    }
  }

  @Override
  public <T, U extends T> U newClassInstance(Class<T> classType, Class<U> classToInstantiate)
      throws InstantiationException, IllegalAccessException {
    initInjector(config.framework());
    U t;
    if (injector != null) {
      t = injector.getInstance(classToInstantiate);
    } else {
      logger.warn("No Guice Injector found in current ServletContext?");
      logger.trace("Unable to find {}. Creating the object directly.", classToInstantiate.getName());
      t = classToInstantiate.newInstance();
    }

    return t;
  }

  @Override
  public String toString() {
    return "Guice ObjectFactory";
  }

  /**
   * Init injector.
   *
   * @param framework the framework
   */
  protected void initInjector(AtmosphereFramework framework) {
    if (injector == null) {
      // start by trying to get an Injector instance from the servlet context
      Injector existingInjector = (Injector) framework.getServletContext().getAttribute(Injector.class.getName());

      AbstractModule[] a = modules.toArray(new AbstractModule[modules.size()]);
      if (existingInjector != null) {
        logger.trace("Adding AtmosphereModule to existing Guice injector");
        injector = existingInjector.createChildInjector(a);
      } else {
        logger.trace("Creating the Guice injector manually with AtmosphereModule");
        injector = Guice.createInjector(a);
      }
    }
  }

  @Override
  public AtmosphereObjectFactory allowInjectionOf(AbstractModule module) {
    modules.add(module);
    return this;
  }

  /**
   * Sets injector.
   *
   * @param injector the injector
   */
  public void setInjector(Injector injector) {
    this.injector = injector;
  }

  private class AtmosphereModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(BroadcasterFactory.class).toProvider(new Provider<BroadcasterFactory>() {
        @Override
        public BroadcasterFactory get() {
          return config.getBroadcasterFactory();
        }
      });
      bind(AtmosphereFramework.class).toProvider(new Provider<AtmosphereFramework>() {
        @Override
        public AtmosphereFramework get() {
          return config.framework();
        }
      });
      bind(AtmosphereResourceFactory.class).toProvider(new Provider<AtmosphereResourceFactory>() {
        @Override
        public AtmosphereResourceFactory get() {
          return config.resourcesFactory();
        }
      });
      bind(MetaBroadcaster.class).toProvider(new Provider<MetaBroadcaster>() {
        @Override
        public MetaBroadcaster get() {
          return config.metaBroadcaster();
        }
      });
      bind(AtmosphereResourceSessionFactory.class).toProvider(new Provider<AtmosphereResourceSessionFactory>() {
        @Override
        public AtmosphereResourceSessionFactory get() {
          return config.sessionFactory();
        }
      });
      bind(AtmosphereConfig.class).toProvider(new Provider<AtmosphereConfig>() {
        @Override
        public AtmosphereConfig get() {
          return config;
        }
      });
    }
  }
}
