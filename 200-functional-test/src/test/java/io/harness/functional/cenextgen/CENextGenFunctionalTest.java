package io.harness.functional.cenextgen;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.CategoryTest;
import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.testframework.framework.CENextGenManagerExecutor;

import io.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CENextGenFunctionalTest extends CategoryTest {
  @BeforeClass
  public static void setup() {
    RestAssured.useRelaxedHTTPSValidation();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(FunctionalTests.class)
  public void shouldEnsureCENextGenManagerStartsUp() {
    assertThatCode(() -> CENextGenManagerExecutor.ensureServiceIsRunning(getClass())).doesNotThrowAnyException();
  }
}
