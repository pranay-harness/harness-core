/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration.setup.rest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsIntegrationTestConstants.API_BASE;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_APP_KEY;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_APP_NAME;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.PageResponse;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;

import software.wings.beans.Application;
import software.wings.integration.UserResourceRestClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class AppResourceRestClient {
  @Inject private UserResourceRestClient userResourceRestClient;

  private ConcurrentHashMap<String, Application> cachedEntity = new ConcurrentHashMap<>();

  public Application getSeedApplication(Client client) {
    return cachedEntity.computeIfAbsent(SEED_APP_KEY, key -> readOrCreateSeedApplication(client));
  }

  public Application readOrCreateSeedApplication(Client client) {
    Application seedApp = getAppByName(client, userResourceRestClient.getUserToken(client),
        userResourceRestClient.getSeedAccount(client).getUuid(), SEED_APP_NAME);
    if (seedApp == null) {
      log.info("Creating SeedApp");
      seedApp = createApp(client, userResourceRestClient.getUserToken(client),
          userResourceRestClient.getSeedAccount(client).getUuid(), SEED_APP_NAME);
    }
    return seedApp;
  }

  public Application getAppByName(Client client, String userToken, String accountId, String appName) {
    WebTarget target = null;
    try {
      target =
          client.target(API_BASE + "/apps?accountId=" + accountId + "&name=" + URLEncoder.encode(appName, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new WingsException(e);
    }
    RestResponse<PageResponse<Application>> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<PageResponse<Application>>>() {});
    return isEmpty(response.getResource()) ? null : response.getResource().get(0);
  }

  public Application createApp(Client client, String userToken, String accountId, String appName) {
    WebTarget target = client.target(API_BASE + "/apps?accountId=" + accountId);
    RestResponse<Application> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .post(entity(anApplication().name(appName).description(appName).accountId(accountId).build(),
                      APPLICATION_JSON),
                new GenericType<RestResponse<Application>>() {});
    assertThat(response.getResource()).isInstanceOf(Application.class);
    assertThat(response.getResource().getName()).isEqualTo(appName);
    return response.getResource();
  }
}
