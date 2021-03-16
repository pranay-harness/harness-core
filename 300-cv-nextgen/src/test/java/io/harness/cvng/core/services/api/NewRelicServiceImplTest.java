package io.harness.cvng.core.services.api;

import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NewRelicServiceImplTest extends CvNextGenTestBase {
  @Inject private NewRelicService newRelicService;

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetEndpoints() {
    List<String> endpoints = newRelicService.getNewRelicEndpoints();

    assertThat(endpoints.size()).isEqualTo(2);
    assertThat(endpoints).contains("https://insights-api.newrelic.com/", "https://insights-api.eu.newrelic.com/");
  }
}
