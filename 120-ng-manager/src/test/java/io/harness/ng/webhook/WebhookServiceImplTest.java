package io.harness.ng.webhook;

import static io.harness.rule.OwnerRule.HARI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WebhookServiceImplTest extends CategoryTest {
  @Before
  public void setup() {}

  @Test
  @Owner(developers = HARI)
  @Category(UnitTests.class)
  public void getTargetUrlTest() throws MalformedURLException {
    final String baseUrl1 = "https://app.harness.io";
    final String baseUrl2 = "https://app.harness.io/gateway/ng/api/";
    final String endpointUrl = "webhookEvent";
    final String finalUrl1 = "https://app.harness.io/webhookEvent";
    final String finalUrl2 = "https://app.harness.io/gateway/ng/api/webhookEvent";
    URL base1 = new URL(baseUrl1);
    URL base2 = new URL(baseUrl2);
    URL targetURL1 = new URL(base1, endpointUrl);
    URL targetURL2 = new URL(base2, endpointUrl);
    assertThat(targetURL1.toString()).isEqualTo(finalUrl1);
    assertThat(targetURL2.toString()).isEqualTo(finalUrl2);
  }
}
