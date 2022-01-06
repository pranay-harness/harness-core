/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration.setup.rest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.integration.SeedData.randomText;
import static software.wings.utils.ArtifactType.DOCKER;
import static software.wings.utils.ArtifactType.WAR;
import static software.wings.utils.WingsIntegrationTestConstants.API_BASE;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_SERVICE_DOCKER_NAME;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_SERVICE_WAR_KEY;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_SERVICE_WAR_NAME;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.PageResponse;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;

import software.wings.beans.Service;
import software.wings.integration.UserResourceRestClient;
import software.wings.utils.ArtifactType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

@Singleton
public class ServiceResourceRestClient {
  @Inject private UserResourceRestClient userResourceRestClient;
  @Inject private AppResourceRestClient appResourceRestClient;

  private ConcurrentHashMap<String, Service> cachedEntity = new ConcurrentHashMap<>();

  public Service getSeedWarService(Client client) {
    return cachedEntity.computeIfAbsent(
        SEED_SERVICE_WAR_KEY, key -> getSeedService(client, SEED_SERVICE_WAR_NAME, WAR));
  }

  public Service getSeedDockerService(Client client) {
    return cachedEntity.computeIfAbsent(
        SEED_SERVICE_WAR_KEY, key -> getSeedService(client, SEED_SERVICE_DOCKER_NAME, DOCKER));
  }

  private Service getSeedService(Client client, String seedServiceName, ArtifactType artifactType) {
    String appId = appResourceRestClient.getSeedApplication(client).getUuid();
    Service service = getServiceByName(client, userResourceRestClient.getUserToken(client), appId, seedServiceName);
    if (service == null) {
      Map<String, Object> serviceMap = new HashMap<>();
      serviceMap.put("name", seedServiceName);
      serviceMap.put("description", randomText(40));
      serviceMap.put("appId", appId);
      serviceMap.put("artifactType", artifactType.name());
      service = createService(client, userResourceRestClient.getUserToken(client), appId, serviceMap);
    }
    return service;
  }

  public Service getServiceByName(Client client, String userToken, String appId, String name) {
    WebTarget target = null;
    try {
      target = client.target(API_BASE + "/services/?appId=" + appId + "&name=" + URLEncoder.encode(name, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      new WingsException(e);
    }

    RestResponse<PageResponse<Service>> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<PageResponse<Service>>>() {});
    return isEmpty(response.getResource()) ? null : response.getResource().get(0);
  }

  public Service getService(Client client, String userToken, String appId, String serviceId) {
    WebTarget target = client.target(API_BASE + "/services/" + serviceId + "?appId=" + appId);

    RestResponse<Service> response = userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
                                         .get(new GenericType<RestResponse<Service>>() {});
    return response.getResource();
  }

  public Service createService(Client client, String userToken, String appId, Map<String, Object> serviceMap) {
    WebTarget target = client.target(API_BASE + "/services/?appId=" + appId);

    RestResponse<Service> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .post(entity(serviceMap, APPLICATION_JSON), new GenericType<RestResponse<Service>>() {});
    assertThat(response.getResource()).isNotNull().isInstanceOf(Service.class);
    String serviceId = response.getResource().getUuid();
    assertThat(serviceId).isNotEmpty();
    Service service = getService(client, userToken, appId, serviceId);
    assertThat(service)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", serviceId)
        .hasFieldOrPropertyWithValue("name", (String) serviceMap.get("name"));
    return service;
  }

  public void deleteService(Client client, String userToken, String serviceId) {
    WebTarget target = client.target(API_BASE + "/services/?appId=" + serviceId);
    RestResponse response = userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
                                .delete(new GenericType<RestResponse>() {

                                });
    assertThat(response).isNotNull();
  }
}
