package software.wings.integration;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.HARNESS_BAMBOO;
import static software.wings.utils.WingsTestConstants.HARNESS_DOCKER_REGISTRY;
import static software.wings.utils.WingsTestConstants.HARNESS_JENKINS;
import static software.wings.utils.WingsTestConstants.HARNESS_NEXUS;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.rule.RepeatRule.Repeat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.config.NexusConfig;
import software.wings.common.Constants;
import software.wings.generator.SecretGenerator;
import software.wings.generator.SecretGenerator.SecretName;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

public class SettingServiceIntegrationTest extends BaseIntegrationTest {
  @Inject SecretGenerator secretGenerator;
  String JENKINS_URL = "https://jenkins.wings.software";
  String JENKINS_USERNAME = "wingsbuild";

  String NEXUS_URL = "https://nexus2.harness.io";
  String NEXUS_USERNAME = "admin";

  String BAMBOO_URL = "http://ec2-18-208-86-222.compute-1.amazonaws.com:8085/";
  String BAMBOO_USERNAME = "wingsbuild";

  String DOCKER_REGISTRY_URL = "https://registry.hub.docker.com/v2/";
  String DOCKER_USERNAME = "wingsplugins";

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  // TODO: Jenkins test need to be looked into later. Test is passing locally, however failing in Jenkins K8s after PR
  // push.
  @Test
  @Ignore
  @Repeat(times = 5)
  public void shouldSaveJenkinsConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(entity(aSettingAttribute()
                             .withName(HARNESS_JENKINS + System.currentTimeMillis())
                             .withCategory(Category.CONNECTOR)
                             .withAccountId(accountId)
                             .withValue(
                                 JenkinsConfig.builder()
                                     .accountId(accountId)
                                     .jenkinsUrl(JENKINS_URL)
                                     .username(JENKINS_USERNAME)
                                     .password(secretGenerator.decryptToCharArray(new SecretName("harness_jenkins")))
                                     .authMechanism(Constants.USERNAME_PASSWORD_FIELD)
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
  public void shouldThrowExceptionForUnreachableJenkinsUrl() {
    Response response =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(entity(
                aSettingAttribute()
                    .withName(HARNESS_JENKINS + System.currentTimeMillis())
                    .withCategory(Category.CONNECTOR)
                    .withAccountId(accountId)
                    .withValue(JenkinsConfig.builder()
                                   .accountId(accountId)
                                   .jenkinsUrl("BAD_URL")
                                   .username(JENKINS_USERNAME)
                                   .password(secretGenerator.decryptToCharArray(new SecretName("harness_jenkins")))
                                   .authMechanism(Constants.USERNAME_PASSWORD_FIELD)
                                   .build())
                    .build(),
                APPLICATION_JSON));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.readEntity(RestResponse.class).getResponseMessages())
        .containsExactly(aResponseMessage()
                             .code(ErrorCode.INVALID_REQUEST)
                             .message("Invalid request: No delegates could reach the resource. [BAD_URL]")
                             .level(Level.ERROR)
                             .build());
  }

  @Test
  public void shouldSaveNexusConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(
                entity(aSettingAttribute()
                           .withName(HARNESS_NEXUS + System.currentTimeMillis())
                           .withCategory(Category.CONNECTOR)
                           .withAccountId(accountId)
                           .withValue(NexusConfig.builder()
                                          .nexusUrl(NEXUS_URL)
                                          .username(NEXUS_USERNAME)
                                          .password(secretGenerator.decryptToCharArray(new SecretName("harness_nexus")))
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
  public void shouldSaveBambooConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(entity(
                      aSettingAttribute()
                          .withName(HARNESS_BAMBOO + System.currentTimeMillis())
                          .withCategory(Category.CONNECTOR)
                          .withAccountId(accountId)
                          .withValue(BambooConfig.builder()
                                         .accountId(accountId)
                                         .bambooUrl(BAMBOO_URL)
                                         .username(BAMBOO_USERNAME)
                                         .password(secretGenerator.decryptToCharArray(new SecretName("harness_bamboo")))
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
  @Repeat(times = 5, successes = 1)
  public void shouldSaveDockerConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(entity(aSettingAttribute()
                             .withName(HARNESS_DOCKER_REGISTRY + System.currentTimeMillis())
                             .withCategory(Category.CONNECTOR)
                             .withAccountId(accountId)
                             .withValue(
                                 DockerConfig.builder()
                                     .accountId(accountId)
                                     .dockerRegistryUrl(DOCKER_REGISTRY_URL)
                                     .username(DOCKER_USERNAME)
                                     .password(secretGenerator.decryptToCharArray(new SecretName("harness_docker_hub")))
                                     .build())
                             .build(),
                      APPLICATION_JSON),
                new GenericType<RestResponse<SettingAttribute>>() {});
    assertThat(restResponse.getResource())
        .isInstanceOf(SettingAttribute.class)
        .extracting("value")
        .extracting("dockerRegistryUrl", "username", "password", "accountId")
        .contains(tuple(DOCKER_REGISTRY_URL, DOCKER_USERNAME, null, accountId));
  }

  private WebTarget getListWebTarget(String accountId) {
    return client.target(format("%s/settings/?accountId=%s", API_BASE, accountId));
  }
}
