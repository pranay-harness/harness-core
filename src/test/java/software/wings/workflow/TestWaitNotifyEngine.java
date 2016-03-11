package software.wings.workflow;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import software.wings.dl.MongoConnectionFactory;
import software.wings.dl.WingsMongoPersistence;

public class TestWaitNotifyEngine {
  static Map<String, Serializable> responseMap = new HashMap<>();

  @Test
  public void testWaitNotify() throws InterruptedException {
    MongoConnectionFactory factory = new MongoConnectionFactory();
    factory.setDb("test");
    factory.setHost("localhost");
    factory.setPort(27017);

    WingsMongoPersistence wingsPersistence = new WingsMongoPersistence(factory.getDatastore());
    WaitNotifyEngine.init(wingsPersistence);

    WaitNotifyEngine waitNotifyEngine = WaitNotifyEngine.getInstance();
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
  public void notify(Map<String, ? extends Serializable> response) {
    System.out.println("TestNotifyCallback-notify " + response);
    TestWaitNotifyEngine.responseMap.putAll(response);
  }
}