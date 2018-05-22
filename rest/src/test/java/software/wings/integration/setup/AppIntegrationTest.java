package software.wings.integration.setup;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.setup.rest.AppResourceRestClient;

public class AppIntegrationTest extends BaseIntegrationTest {
  @Inject private AppResourceRestClient appResourceRestClient;

  @Test
  @Ignore // TODO: It looks like that it is starting to early before Portal starts
  public void shouldReturnSeedApplication() {
    Application seedApplication = appResourceRestClient.getSeedApplication(client);
    assertThat(seedApplication)
        .isNotNull()
        .hasFieldOrProperty("uuid")
        .hasFieldOrPropertyWithValue("name", SEED_APP_NAME);
  }
}
