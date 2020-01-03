package software.wings.integration.appdynamics;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.RAGHU;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.api.HostElement;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.WorkflowExecution;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsNode;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 5/11/17.
 */
@Slf4j
public class AppdynamicsIntegrationTest extends BaseIntegrationTest {
  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;
  @Inject private ScmSecret scmSecret;
  @Mock private EncryptionService encryptionService;
  private String appdynamicsSettingId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    SettingAttribute appdSettingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName("AppDynamics" + System.currentTimeMillis())
            .withAccountId(accountId)
            .withValue(AppDynamicsConfig.builder()
                           .accountId(accountId)
                           .controllerUrl("https://harness-test.saas.appdynamics.com/controller")
                           .username("raghu@harness.io")
                           .accountname("harness-test")
                           .password(scmSecret.decryptToCharArray(new SecretName("appd_config_password")))
                           .build())
            .build();
    appdynamicsSettingId = wingsPersistence.saveAndGet(SettingAttribute.class, appdSettingAttribute).getUuid();
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  public void testGetAllApplications() throws Exception {
    // get all applications
    WebTarget target = client.target(
        API_BASE + "/appdynamics/applications?settingId=" + appdynamicsSettingId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    assertThat(restResponse.getResponseMessages()).isEmpty();
    assertThat(restResponse.getResource().isEmpty()).isFalse();

    for (NewRelicApplication app : restResponse.getResource()) {
      assertThat(app.getId() > 0).isTrue();
      assertThat(isBlank(app.getName())).isFalse();
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  public void testGetAllTiers() throws Exception {
    // get all applications
    WebTarget target = client.target(
        API_BASE + "/appdynamics/applications?settingId=" + appdynamicsSettingId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    int maxAppToTest = 0;
    for (NewRelicApplication application : restResponse.getResource()) {
      WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettingId
          + "&accountId=" + accountId + "&appdynamicsAppId=" + application.getId());
      RestResponse<List<AppdynamicsTier>> tierRestResponse =
          getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
      assertThat(tierRestResponse.getResource().isEmpty()).isFalse();

      for (AppdynamicsTier tier : tierRestResponse.getResource()) {
        assertThat(tier.getId() > 0).isTrue();
        assertThat(isBlank(tier.getName())).isFalse();
        assertThat(isBlank(tier.getType())).isFalse();
        assertThat(isBlank(tier.getAgentType())).isFalse();
        assertThat(tier.getName().isEmpty()).isFalse();
      }

      if (++maxAppToTest >= 5) {
        break;
      }
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testGetAllTierBTMetrics() throws Exception {
    SettingAttribute appdSettingAttribute = settingsService.get(appdynamicsSettingId);
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) appdSettingAttribute.getValue();

    // get all applications
    WebTarget target = client.target(
        API_BASE + "/appdynamics/applications?settingId=" + appdynamicsSettingId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    for (NewRelicApplication application : restResponse.getResource()) {
      WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettingId
          + "&accountId=" + accountId + "&appdynamicsAppId=" + application.getId());
      RestResponse<List<AppdynamicsTier>> tierRestResponse =
          getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
      assertThat(tierRestResponse.getResource().isEmpty()).isFalse();

      for (AppdynamicsTier tier : tierRestResponse.getResource()) {
        List<AppdynamicsMetric> btMetrics = appdynamicsDelegateService.getTierBTMetrics(appDynamicsConfig,
            application.getId(), tier.getId(), secretManager.getEncryptionDetails(appDynamicsConfig, null, null),
            createApiCallLog(appDynamicsConfig.getAccountId(), null));

        assertThat(btMetrics.isEmpty()).isFalse();

        for (AppdynamicsMetric btMetric : btMetrics) {
          assertThat(isBlank(btMetric.getName())).isFalse();
          assertThat(btMetric.getChildMetrices().isEmpty()).isFalse();

          for (AppdynamicsMetric leafMetric : btMetric.getChildMetrices()) {
            assertThat(isBlank(leafMetric.getName())).isFalse();
            assertThat(leafMetric.getChildMetrices().size()).isEqualTo(0);
          }
        }
      }
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  public void testGetDependentTiers() throws IOException {
    WebTarget target = client.target(
        API_BASE + "/appdynamics/applications?settingId=" + appdynamicsSettingId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    int maxAppToTest = 0;
    for (NewRelicApplication application : restResponse.getResource()) {
      WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettingId
          + "&accountId=" + accountId + "&appdynamicsAppId=" + application.getId());
      RestResponse<List<AppdynamicsTier>> tierRestResponse =
          getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
      assertThat(tierRestResponse.getResource().isEmpty()).isFalse();

      for (AppdynamicsTier tier : tierRestResponse.getResource()) {
        assertThat(tier.getId() > 0).isTrue();
        assertThat(isBlank(tier.getName())).isFalse();
        assertThat(isBlank(tier.getType())).isFalse();
        assertThat(isBlank(tier.getAgentType())).isFalse();
        assertThat(tier.getName().isEmpty()).isFalse();

        WebTarget dependentTarget =
            client.target(API_BASE + "/appdynamics/dependent-tiers?settingId=" + appdynamicsSettingId
                + "&accountId=" + accountId + "&appdynamicsAppId=" + application.getId() + "&tierId=" + tier.getId());
        RestResponse<Set<AppdynamicsTier>> dependentTierResponse =
            getRequestBuilderWithAuthHeader(dependentTarget).get(new GenericType<RestResponse<Set<AppdynamicsTier>>>() {
            });
        logger.info("" + dependentTierResponse.getResource());
      }
      if (++maxAppToTest >= 5) {
        break;
      }
    }
  }

  @Test
  @Owner(developers = KAMAL, intermittent = true)
  @Category(IntegrationTests.class)
  public void testGetDataForNode() throws Exception {
    String appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    String workflowId = wingsPersistence.save(aWorkflow().appId(appId).name(generateUuid()).build());
    String workflowExecutionId = wingsPersistence.save(
        WorkflowExecution.builder().appId(appId).workflowId(workflowId).status(ExecutionStatus.SUCCESS).build());
    wingsPersistence.save(aStateExecutionInstance()
                              .executionUuid(workflowExecutionId)
                              .stateType(StateType.PHASE.name())
                              .appId(appId)
                              .displayName(generateUuid())
                              .build());

    AppDynamicsConfig appDynamicsConfig =
        (AppDynamicsConfig) wingsPersistence.get(SettingAttribute.class, appdynamicsSettingId).getValue();
    // get all applications
    WebTarget target = client.target(
        API_BASE + "/appdynamics/applications?settingId=" + appdynamicsSettingId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    final AtomicInteger numOfMetricsData = new AtomicInteger(0);
    int numOfNodesExamined = 0;
    for (NewRelicApplication application : restResponse.getResource()) {
      if (!application.getName().equals("cv-app")) {
        continue;
      }
      WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettingId
          + "&accountId=" + accountId + "&appdynamicsAppId=" + application.getId());
      RestResponse<List<AppdynamicsTier>> tierRestResponse =
          getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
      assertThat(tierRestResponse.getResource().isEmpty()).isFalse();

      for (AppdynamicsTier tier : tierRestResponse.getResource()) {
        if (!tier.getName().equals("docker-tier")) {
          continue;
        }
        assertThat(tier.getId() > 0).isTrue();
        assertThat(application.getId() > 0).isTrue();
        logger.info(application.toString());
        Set<AppdynamicsNode> nodes = appdynamicsDelegateService.getNodes(appDynamicsConfig, application.getId(),
            tier.getId(), secretManager.getEncryptionDetails(appDynamicsConfig, null, null),
            createApiCallLog(appDynamicsConfig.getAccountId(), null));

        for (AppdynamicsNode node : new TreeSet<>(nodes).descendingSet()) {
          AppdynamicsSetupTestNodeData testNodeData =
              AppdynamicsSetupTestNodeData.builder()
                  .applicationId(application.getId())
                  .tierId(tier.getId())
                  .settingId(appdynamicsSettingId)
                  .appId(appId)
                  .guid("test_guid")
                  .instanceName(generateUuid())
                  .hostExpression("${host.hostName}")
                  .workflowId(workflowId)
                  .instanceElement(anInstanceElement()
                                       .host(HostElement.Builder.aHostElement().hostName(node.getName()).build())
                                       .build())
                  .build();
          try {
            target = client.target(API_BASE + "/appdynamics/node-data?accountId=" + accountId);
            RestResponse<VerificationNodeDataSetupResponse> metricResponse =
                getRequestBuilderWithAuthHeader(target).post(entity(testNodeData, APPLICATION_JSON),
                    new GenericType<RestResponse<VerificationNodeDataSetupResponse>>() {});

            assertThat(metricResponse.getResponseMessages()).isEmpty();
            assertThat(metricResponse.getResource().isProviderReachable()).isTrue();
            assertThat(metricResponse.getResource().getLoadResponse().isLoadPresent()).isTrue();
            assertThat(metricResponse.getResource().getLoadResponse().getLoadResponse()).isNotNull();
            final List<AppdynamicsMetric> tierMetrics =
                (List<AppdynamicsMetric>) metricResponse.getResource().getLoadResponse().getLoadResponse();
            assertThat(tierMetrics.isEmpty()).isFalse();

            List<AppdynamicsMetricData> metricsDatas =
                JsonUtils.asObject(JsonUtils.asJson(metricResponse.getResource().getDataForNode()),
                    new TypeReference<List<AppdynamicsMetricData>>() {});
            //              (List<AppdynamicsMetricData>) metricResponse.getResource().getDataForNode();
            metricsDatas.forEach(metricsData -> {
              if (!EmptyPredicate.isEmpty(metricsData.getMetricValues())) {
                numOfMetricsData.addAndGet(metricsData.getMetricValues().size());
              }
            });

            if (numOfMetricsData.get() > 0) {
              logger.info("got data for node {} tier {} app {}", node.getName(), tier.getName(), application.getName());
              return;
            }

            if (++numOfNodesExamined > 20) {
              logger.info("did not get data for any node");
              return;
            }
            logger.info("examined node {} so far {}", node.getName(), numOfNodesExamined);
          } catch (Exception e) {
            logger.error("Exception while running test ", e);
            // TODO: find the issue in jenkins PR env and remove this try catch.
          }
        }
      }
    }
  }
}
