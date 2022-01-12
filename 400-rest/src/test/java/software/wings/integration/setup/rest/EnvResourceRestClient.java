/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration.setup.rest;

import static io.harness.beans.EnvironmentType.NON_PROD;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.integration.SeedData.randomText;
import static software.wings.utils.WingsIntegrationTestConstants.API_BASE;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_ENV_KEY;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_ENV_NAME;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_FAKE_HOSTS;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_FAKE_HOSTS_DC_INFRA_KEY;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_FAKE_HOSTS_INFRA_NAME;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.PageResponse;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;

import software.wings.api.DeploymentType;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.ServiceTemplate;
import software.wings.integration.UserResourceRestClient;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

@Singleton
public class EnvResourceRestClient {
  @Inject private UserResourceRestClient userResourceRestClient;
  @Inject private AppResourceRestClient appResourceRestClient;
  @Inject private SettingsResourceRestClient settingsResourceRestClient;
  @Inject private ServiceResourceRestClient serviceResourceRestClient;

  private ConcurrentHashMap<String, Environment> envCachedEntity = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String, InfrastructureMapping> infraCachedEntity = new ConcurrentHashMap<>();

  public Environment getSeedEnvironment(Client client) {
    return envCachedEntity.computeIfAbsent(SEED_ENV_KEY, key -> fetchOrCreateEnvironment(client));
  }

  private Environment fetchOrCreateEnvironment(Client client) {
    String appId = appResourceRestClient.getSeedApplication(client).getUuid();
    Environment seedEnv = getEnvByName(client, userResourceRestClient.getUserToken(client), appId, SEED_ENV_NAME);
    if (seedEnv == null) {
      Map<String, Object> envMap = new HashMap<>();
      envMap.put("name", SEED_ENV_NAME);
      envMap.put("description", randomText(40));
      envMap.put("appId", appId);
      envMap.put("environmentType", NON_PROD);

      seedEnv = createEnv(client, userResourceRestClient.getUserToken(client), appId, envMap);
    }
    return seedEnv;
  }

  public Environment getEnvByName(Client client, String userToken, String appId, String name) {
    WebTarget target = null;
    try {
      target = client.target(API_BASE + "/environments/?appId=" + appId + "&name=" + URLEncoder.encode(name, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new WingsException(e);
    }

    RestResponse<PageResponse<Environment>> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<PageResponse<Environment>>>() {});
    return isEmpty(response.getResource()) ? null : response.getResource().get(0);
  }

  public Environment createEnv(Client client, String userToken, String appId, Map<String, Object> envMap) {
    WebTarget target = client.target(API_BASE + "/environments/?appId=" + appId);

    RestResponse<Environment> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .post(entity(envMap, APPLICATION_JSON), new GenericType<RestResponse<Environment>>() {});
    assertThat(response.getResource()).isNotNull().isInstanceOf(Environment.class);
    String envId = response.getResource().getUuid();
    assertThat(envId).isNotEmpty();
    Environment env = getEnvByName(client, userToken, appId, (String) envMap.get("name"));
    assertThat(env)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", envId)
        .hasFieldOrPropertyWithValue("name", (String) envMap.get("name"));
    return env;
  }

  public Environment createEnv(Client client, String userToken, String appId, Environment environment) {
    WebTarget target = client.target(API_BASE + "/environments/?appId=" + appId);
    RestResponse<Environment> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .post(entity(environment, APPLICATION_JSON), new GenericType<RestResponse<Environment>>() {});
    assertThat(response.getResource()).isNotNull().isInstanceOf(Environment.class);
    return response.getResource();
  }

  public InfrastructureMapping getSeedFakeHostsDcInfra(Client client) {
    return infraCachedEntity.computeIfAbsent(
        SEED_FAKE_HOSTS_DC_INFRA_KEY, key -> fetchOrCreateFakeHostsDcInfra(client));
  }

  private InfrastructureMapping fetchOrCreateFakeHostsDcInfra(Client client) {
    String appId = appResourceRestClient.getSeedApplication(client).getUuid();
    String envId = getSeedEnvironment(client).getUuid();
    String serviceId = serviceResourceRestClient.getSeedWarService(client).getUuid();
    String tesmplateId =
        getServiceTemplate(client, userResourceRestClient.getUserToken(client), appId, envId, serviceId).getUuid();
    InfrastructureMapping seedFakeHostsInfraMapping = getInfrastructureMappingByName(
        client, userResourceRestClient.getUserToken(client), appId, envId, SEED_FAKE_HOSTS_INFRA_NAME);
    if (seedFakeHostsInfraMapping == null) {
      seedFakeHostsInfraMapping =
          createInfrastructureMapping(client, userResourceRestClient.getUserToken(client), appId, envId,
              aPhysicalInfrastructureMapping()
                  .withComputeProviderSettingId(settingsResourceRestClient.seedDataCenter(client).getUuid())
                  .withHostNames(Arrays.asList(SEED_FAKE_HOSTS.split(",")))
                  .withName(SEED_FAKE_HOSTS_INFRA_NAME)
                  .withInfraMappingType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name())
                  .withDeploymentType(DeploymentType.SSH.name())
                  .withComputeProviderType(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
                  .withHostConnectionAttrs(settingsResourceRestClient.seedSshKey(client).getName())
                  .withServiceTemplateId(tesmplateId)
                  .withAutoPopulate(false)
                  .build());
    }
    return seedFakeHostsInfraMapping;
  }

  public InfrastructureMapping getInfrastructureMapping(
      Client client, String userToken, String appId, String envId, String infrastructureMappingId) {
    WebTarget target = client.target(
        API_BASE + "/infrastructure-mappings/" + infrastructureMappingId + "?appId=" + appId + "&envId=" + envId);
    RestResponse<InfrastructureMapping> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<InfrastructureMapping>>() {});
    assertThat(response.getResource()).isNotNull().isInstanceOf(InfrastructureMapping.class);
    return response.getResource();
  }

  public InfrastructureMapping getInfrastructureMappingByName(
      Client client, String userToken, String appId, String envId, String name) {
    WebTarget target = client.target(API_BASE + "/infrastructure-mappings/?appId=" + appId + "&envId=" + envId);
    RestResponse<PageResponse<InfrastructureMapping>> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<PageResponse<InfrastructureMapping>>>() {});
    return isEmpty(response.getResource()) ? null : response.getResource().get(0);
  }

  public InfrastructureMapping createInfrastructureMapping(
      Client client, String userToken, String appId, String envId, InfrastructureMapping infrastructureMapping) {
    WebTarget target = client.target(API_BASE + "/infrastructure-mappings/?appId=" + appId + "&envId=" + envId);
    RestResponse<InfrastructureMapping> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .post(entity(infrastructureMapping, APPLICATION_JSON),
                new GenericType<RestResponse<InfrastructureMapping>>() {});
    assertThat(response.getResource()).isNotNull().isInstanceOf(InfrastructureMapping.class);
    String infrastructureMappingId = response.getResource().getUuid();
    assertThat(infrastructureMappingId).isNotEmpty();
    InfrastructureMapping fetched = getInfrastructureMapping(client, userToken, appId, envId, infrastructureMappingId);
    assertThat(fetched)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", infrastructureMappingId)
        .hasFieldOrPropertyWithValue("name", infrastructureMapping.getName());
    return fetched;
  }

  public ServiceTemplate getServiceTemplate(
      Client client, String userToken, String appId, String envId, String serviceId) {
    WebTarget target =
        client.target(API_BASE + "/service-templates?appId=" + appId + "&envId=" + envId + "&serviceId=" + serviceId);

    RestResponse<PageResponse<ServiceTemplate>> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<PageResponse<ServiceTemplate>>>() {});
    return isEmpty(response.getResource()) ? null : response.getResource().get(0);
  }
}
