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

import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.dl.MongoConfig;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.waitNotify.NotifyCallback;
import software.wings.waitNotify.WaitNotifyEngine;

import javax.inject.Inject;

public class TestWaitNotifyEngine extends WingsBaseTest {
  static Map<String, Serializable> responseMap = new HashMap<>();

  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Test
  public void testWaitNotify() throws InterruptedException {
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