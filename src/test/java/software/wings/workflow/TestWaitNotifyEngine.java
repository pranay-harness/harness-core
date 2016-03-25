package software.wings.workflow;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import software.wings.dl.MongoConnectionFactory;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.waitNotify.NotifyCallback;
import software.wings.waitNotify.WaitNotifyEngine;

public class TestWaitNotifyEngine {
  static Map<String, Serializable> responseMap = new HashMap<>();

  @Test
  public void testWaitNotify() throws InterruptedException {
    MongoConnectionFactory factory = new MongoConnectionFactory();
    factory.setDb("test");
    factory.setHost("localhost");
    factory.setPort(27017);

    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(MongoConnectionFactory.class).toInstance(factory);
        bind(WingsPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
      }
    });

    WaitNotifyEngine waitNotifyEngine = injector.getInstance(WaitNotifyEngine.class);
    waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123", "345");

    System.out.println("responseMap:" + responseMap);
    Thread.sleep(5000);
    waitNotifyEngine.notify("123", "response-123");
    Thread.sleep(5000);
    System.out.println("responseMap:" + responseMap);
    waitNotifyEngine.notify("345", "response-345");
    Thread.sleep(5000);
    System.out.println("responseMap:" + responseMap);

    Assert.assertEquals("response-123", responseMap.get("123"));
    Assert.assertEquals("response-345", responseMap.get("345"));
    System.out.println("All Done");
  }
}

class TestNotifyCallback implements NotifyCallback {
  @Override
  public void notify(Map<String, Serializable> response) {
    System.out.println("TestNotifyCallback-notify " + response);
    TestWaitNotifyEngine.responseMap.putAll(response);
  }
}