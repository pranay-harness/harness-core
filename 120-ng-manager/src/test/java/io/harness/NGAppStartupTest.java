package io.harness;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.harness.category.element.UnitTests;
import io.harness.ng.NextGenApplication;
import io.harness.ng.NextGenConfiguration;
import io.harness.rule.Owner;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class NGAppStartupTest {
  @ClassRule
  public static final DropwizardAppRule<NextGenConfiguration> RULE =
      new DropwizardAppRule<>(NextGenApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  @Ignore("Working locally, need to find a way to get it working on Jenkins")
  public void testAppStartup() {
    final Client client = new JerseyClientBuilder().build();
    final Response response =
        client.target(String.format("http://localhost:%d/swagger.json", RULE.getLocalPort())).request().get();
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    response.close();
  }
}
