package io.harness.functional.eventserver;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.CategoryTest;
import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.testframework.framework.EventServerExecutor;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EventServerFunctionalTest extends CategoryTest {
  private static final String ALPN_JAR =
      "org/mortbay/jetty/alpn/alpn-boot/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar";
  private static String ALPN = "/home/jenkins/maven-repositories/0/";

  private static EventServerExecutor eventServerExecutor = new EventServerExecutor();

  @Test
  @Owner(developers = AVMOHAN)
  @Category(FunctionalTests.class)
  public void shouldEnsureEventServer() throws Exception {
    assertThatCode(() -> {
      eventServerExecutor.ensureEventServer(EventServerFunctionalTest.class, ALPN, ALPN_JAR);
    }).doesNotThrowAnyException();
  }
}
