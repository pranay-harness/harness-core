package software.wings.integration.setup.rest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsIntegrationTestConstants.API_BASE;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_APP_KEY;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_APP_NAME;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.integration.UserResourceRestClient;

import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

@Singleton
public class AppResourceRestClient {
  private static final Logger logger = LoggerFactory.getLogger(AppResourceRestClient.class);

  @Inject private UserResourceRestClient userResourceRestClient;

  private ConcurrentHashMap<String, Application> cachedEntity = new ConcurrentHashMap<>();

  public Application getSeedApplication(Client client) {
    return cachedEntity.computeIfAbsent(SEED_APP_KEY, key -> readOrCreateSeedApplication(client));
  }

  public Application readOrCreateSeedApplication(Client client) {
    Application seedApp = getAppByName(client, userResourceRestClient.getUserToken(client),
        userResourceRestClient.getSeedAccount(client).getUuid(), SEED_APP_NAME);
    if (seedApp == null) {
      logger.info("Creating SeedApp");
      seedApp = createApp(client, userResourceRestClient.getUserToken(client),
          userResourceRestClient.getSeedAccount(client).getUuid(), SEED_APP_NAME);
    }
    return seedApp;
  }

  public Application getAppByName(Client client, String userToken, String accountId, String appName) {
    WebTarget target = client.target(API_BASE + "/apps?accountId=" + accountId + "&name=" + URLEncoder.encode(appName));
    RestResponse<PageResponse<Application>> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<PageResponse<Application>>>() {});
    return isEmpty(response.getResource()) ? null : response.getResource().get(0);
  }

  public Application createApp(Client client, String userToken, String accountId, String appName) {
    WebTarget target = client.target(API_BASE + "/apps?accountId=" + accountId);
    RestResponse<Application> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .post(entity(anApplication().withName(appName).withDescription(appName).withAccountId(accountId).build(),
                      APPLICATION_JSON),
                new GenericType<RestResponse<Application>>() {});
    assertThat(response.getResource()).isInstanceOf(Application.class);
    assertThat(response.getResource().getName()).isEqualTo(appName);
    return response.getResource();
  }
}
