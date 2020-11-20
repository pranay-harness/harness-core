package io.harness.functional.ci;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.CategoryTest;
import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.testframework.framework.CIManagerExecutor;
import io.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CIStartupFunctionalTest extends CategoryTest {
  private static final String ALPN_JAR =
      "org/mortbay/jetty/alpn/alpn-boot/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar";
  private static final String ALPN = "/home/jenkins/maven-repositories/0/";

  @BeforeClass
  public static void setup() {
    RestAssured.useRelaxedHTTPSValidation();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(FunctionalTests.class)
  public void shouldEnsureCIManagerStartsUp() {
    assertThatCode(() -> CIManagerExecutor.ensureCIManager(getClass(), ALPN, ALPN_JAR)).doesNotThrowAnyException();
  }
}
