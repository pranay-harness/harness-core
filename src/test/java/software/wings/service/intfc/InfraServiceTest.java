package software.wings.service.intfc;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.junit.Test;
import software.wings.app.MainConfiguration;
import software.wings.beans.Host;
import software.wings.beans.Tag;
import software.wings.dl.MongoConfig;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.InfraServiceImpl;

/**
 * Created by anubhaw on 3/30/16.
 */
public class InfraServiceTest {
  private Injector getInjector() {
    MongoConfig factory = new MongoConfig();
    factory.setDb("wings");
    factory.setHost("localhost");
    factory.setPort(27017);
    MainConfiguration mainConfiguration = new MainConfiguration();
    mainConfiguration.setMongoConnectionFactory(factory);

    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(MainConfiguration.class).toInstance(mainConfiguration);
        bind(WingsPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
        bind(InfraService.class).to(InfraServiceImpl.class);
      }
    });
    return injector;
  }

  Injector injector = getInjector();
  InfraService infraService = injector.getInstance(InfraService.class);

  @Test
  public void testCreateTag() {
    Tag tag = new Tag();
    tag.setType("OS");
    tag.setName("OS");
    tag.setDescription("Operating system types");
    tag = infraService.createTag("ddn", tag);

    Host host = new Host();
    host = infraService.createHost("ff329r", host);

    Host host1 = infraService.applyTag(host.getUuid(), tag.getUuid());
    System.out.println(host);
  }
}