/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration;

import static io.harness.rule.OwnerRule.ANUBHAW;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.HARNESS_BAMBOO;
import static software.wings.utils.WingsTestConstants.HARNESS_DOCKER_REGISTRY;
import static software.wings.utils.WingsTestConstants.HARNESS_JENKINS;
import static software.wings.utils.WingsTestConstants.HARNESS_NEXUS;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.eraro.ResponseMessage;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.config.NexusConfig;

import com.google.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SettingServiceIntegrationTest extends IntegrationTestBase {
  @Inject private ScmSecret scmSecret;

  String JENKINS_URL = "https://jenkins.wings.software";
  String JENKINS_USERNAME = "wingsbuild";

  String NEXUS_URL = "https://nexus2.harness.io";
  String NEXUS_USERNAME = "admin";

  String BAMBOO_URL = "http://ec2-18-208-86-222.compute-1.amazonaws.com:8085/";
  String BAMBOO_USERNAME = "wingsbuild";

  String DOCKER_REGISTRY_URL = "https://registry.hub.docker.com/v2/";
  String DOCKER_USERNAME = "wingsplugins";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  // TODO: Jenkins test need to be looked into later. Test is passing locally, however failing in Jenkins K8s after PR
  // push.
  @Test
  @Owner(developers = ANUBHAW)
  @Repeat(times = 5)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldSaveJenkinsConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(entity(aSettingAttribute()
                             .withName(HARNESS_JENKINS + System.currentTimeMillis())
                             .withCategory(SettingCategory.CONNECTOR)
                             .withAccountId(accountId)
                             .withValue(JenkinsConfig.builder()
                                            .accountId(accountId)
                                            .jenkinsUrl(JENKINS_URL)
                                            .username(JENKINS_USERNAME)
                                            .password(scmSecret.decryptToCharArray(new SecretName("harness_jenkins")))
                                            .authMechanism(JenkinsConfig.USERNAME_DEFAULT_TEXT)
                                            .build())
                             .build(),
                      APPLICATION_JSON),
                new GenericType<RestResponse<SettingAttribute>>() {});

    assertThat(restResponse.getResource())
        .isInstanceOf(SettingAttribute.class)
        .extracting("value")
        .extracting("jenkinsUrl", "username", "password", "accountId")
        .contains(tuple(JENKINS_URL, JENKINS_USERNAME, null, accountId));
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldThrowExceptionForUnreachableJenkinsUrl() {
    Response response =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(entity(aSettingAttribute()
                             .withName(HARNESS_JENKINS + System.currentTimeMillis())
                             .withCategory(SettingCategory.CONNECTOR)
                             .withAccountId(accountId)
                             .withValue(JenkinsConfig.builder()
                                            .accountId(accountId)
                                            .jenkinsUrl("BAD_URL")
                                            .username(JENKINS_USERNAME)
                                            .password(scmSecret.decryptToCharArray(new SecretName("harness_jenkins")))
                                            .authMechanism(JenkinsConfig.USERNAME_DEFAULT_TEXT)
                                            .build())
                             .build(),
                APPLICATION_JSON));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.readEntity(RestResponse.class).getResponseMessages())
        .containsExactly(ResponseMessage.builder()
                             .message("Invalid request: No delegates could reach the resource. [BAD_URL]")
                             .build());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldSaveNexusConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(entity(aSettingAttribute()
                             .withName(HARNESS_NEXUS + System.currentTimeMillis())
                             .withCategory(SettingCategory.CONNECTOR)
                             .withAccountId(accountId)
                             .withValue(NexusConfig.builder()
                                            .nexusUrl(NEXUS_URL)
                                            .username(NEXUS_USERNAME)
                                            .password(scmSecret.decryptToCharArray(new SecretName("harness_nexus")))
                                            .accountId(accountId)
                                            .build())
                             .build(),
                      APPLICATION_JSON),
                new GenericType<RestResponse<SettingAttribute>>() {});
    assertThat(restResponse.getResource())
        .isInstanceOf(SettingAttribute.class)
        .extracting("value")
        .extracting("nexusUrl", "username", "password")
        .contains(tuple(NEXUS_URL, NEXUS_USERNAME, null));
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldSaveBambooConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(entity(aSettingAttribute()
                             .withName(HARNESS_BAMBOO + System.currentTimeMillis())
                             .withCategory(SettingCategory.CONNECTOR)
                             .withAccountId(accountId)
                             .withValue(BambooConfig.builder()
                                            .accountId(accountId)
                                            .bambooUrl(BAMBOO_URL)
                                            .username(BAMBOO_USERNAME)
                                            .password(scmSecret.decryptToCharArray(new SecretName("harness_bamboo")))
                                            .build())
                             .build(),
                      APPLICATION_JSON),
                new GenericType<RestResponse<SettingAttribute>>() {});
    assertThat(restResponse.getResource())
        .isInstanceOf(SettingAttribute.class)
        .extracting("value")
        .extracting("bambooUrl", "username", "password", "accountId")
        .contains(tuple(BAMBOO_URL, BAMBOO_USERNAME, null, accountId));
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Repeat(times = 5, successes = 1)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void shouldSaveDockerConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(
                entity(aSettingAttribute()
                           .withName(HARNESS_DOCKER_REGISTRY + System.currentTimeMillis())
                           .withCategory(SettingCategory.CONNECTOR)
                           .withAccountId(accountId)
                           .withValue(DockerConfig.builder()
                                          .accountId(accountId)
                                          .dockerRegistryUrl(DOCKER_REGISTRY_URL)
                                          .username(DOCKER_USERNAME)
                                          .password(scmSecret.decryptToCharArray(new SecretName("harness_docker_hub")))
                                          .build())
                           .build(),
                    APPLICATION_JSON),
                new GenericType<RestResponse<SettingAttribute>>() {});
    assertThat(restResponse.getResource())
        .isInstanceOf(SettingAttribute.class)
        .extracting("value")
        .extracting("dockerRegistryUrl", "username", "password", "accountId")
        .contains(DOCKER_REGISTRY_URL, DOCKER_USERNAME, null, accountId);
  }

  private WebTarget getListWebTarget(String accountId) {
    return client.target(format("%s/settings/?accountId=%s", API_BASE, accountId));
  }
}
