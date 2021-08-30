package io.harness;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.category.element.StartupTests;
import io.harness.rule.Owner;

import io.restassured.RestAssured;
import java.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NgManagerStartupTest {
  @BeforeClass
  public static void setup() {
    RestAssured.useRelaxedHTTPSValidation();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(StartupTests.class)
  public void shouldEnsureCIManagerStartsUp() {
    final ServiceExecutor serviceExecutor =
        new ServiceExecutor("310-ci-manager", "ci-manager-config.yml", Duration.ofMinutes(5));
    assertThatCode(() -> serviceExecutor.ensureManager(getClass())).doesNotThrowAnyException();
  }
}
