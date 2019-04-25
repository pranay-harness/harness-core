package software.wings.integration.appdynamics;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import io.harness.rule.RepeatRule.Repeat;
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
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  public void testGetAllApplications() throws Exception {
    // get all applications
    WebTarget target = client.target(
        API_BASE + "/appdynamics/applications?settingId=" + appdynamicsSettingId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    assertEquals(0, restResponse.getResponseMessages().size());
    assertFalse(restResponse.getResource().isEmpty());

    for (NewRelicApplication app : restResponse.getResource()) {
      assertTrue(app.getId() > 0);
      assertFalse(isBlank(app.getName()));
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  public void testGetAllTiers() throws Exception {
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
      assertFalse(tierRestResponse.getResource().isEmpty());

      for (AppdynamicsTier tier : tierRestResponse.getResource()) {
        assertTrue(tier.getId() > 0);
        assertFalse(isBlank(tier.getName()));
        assertFalse(isBlank(tier.getType()));
        assertFalse(isBlank(tier.getAgentType()));
        assertFalse(tier.getName().isEmpty());
      }
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  @Ignore
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
      assertFalse(tierRestResponse.getResource().isEmpty());

      for (AppdynamicsTier tier : tierRestResponse.getResource()) {
        List<AppdynamicsMetric> btMetrics = appdynamicsDelegateService.getTierBTMetrics(appDynamicsConfig,
            application.getId(), tier.getId(), secretManager.getEncryptionDetails(appDynamicsConfig, null, null),
            createApiCallLog(appDynamicsConfig.getAccountId(), accountId, null));

        assertFalse(btMetrics.isEmpty());

        for (AppdynamicsMetric btMetric : btMetrics) {
          assertFalse(isBlank(btMetric.getName()));
          assertFalse("failed for " + btMetric.getName(), btMetric.getChildMetrices().isEmpty());

          for (AppdynamicsMetric leafMetric : btMetric.getChildMetrices()) {
            assertFalse(isBlank(leafMetric.getName()));
            assertEquals("failed for " + btMetric.getName() + "|" + leafMetric.getName(), 0,
                leafMetric.getChildMetrices().size());
          }
        }
      }
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  public void testGetDependentTiers() throws IOException {
    WebTarget target = client.target(
        API_BASE + "/appdynamics/applications?settingId=" + appdynamicsSettingId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    for (NewRelicApplication application : restResponse.getResource()) {
      WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettingId
          + "&accountId=" + accountId + "&appdynamicsAppId=" + application.getId());
      RestResponse<List<AppdynamicsTier>> tierRestResponse =
          getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
      assertFalse(tierRestResponse.getResource().isEmpty());

      for (AppdynamicsTier tier : tierRestResponse.getResource()) {
        assertTrue(tier.getId() > 0);
        assertFalse(isBlank(tier.getName()));
        assertFalse(isBlank(tier.getType()));
        assertFalse(isBlank(tier.getAgentType()));
        assertFalse(tier.getName().isEmpty());

        WebTarget dependentTarget =
            client.target(API_BASE + "/appdynamics/dependent-tiers?settingId=" + appdynamicsSettingId
                + "&accountId=" + accountId + "&appdynamicsAppId=" + application.getId() + "&tierId=" + tier.getId());
        RestResponse<Set<AppdynamicsTier>> dependentTierResponse =
            getRequestBuilderWithAuthHeader(dependentTarget).get(new GenericType<RestResponse<Set<AppdynamicsTier>>>() {
            });
        logger.info("" + dependentTierResponse.getResource());
      }
    }
  }

  @Test
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
      assertFalse(tierRestResponse.getResource().isEmpty());

      for (AppdynamicsTier tier : tierRestResponse.getResource()) {
        if (!tier.getName().equals("docker-tier")) {
          continue;
        }
        assertTrue(tier.getId() > 0);
        assertTrue(application.getId() > 0);
        logger.info(application.toString());
        Set<AppdynamicsNode> nodes = appdynamicsDelegateService.getNodes(appDynamicsConfig, application.getId(),
            tier.getId(), secretManager.getEncryptionDetails(appDynamicsConfig, null, null),
            createApiCallLog(appDynamicsConfig.getAccountId(), accountId, null));

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
                  .instanceElement(
                      anInstanceElement()
                          .withHost(HostElement.Builder.aHostElement().withHostName(node.getName()).build())
                          .build())
                  .build();
          target = client.target(API_BASE + "/appdynamics/node-data?accountId=" + accountId);
          RestResponse<VerificationNodeDataSetupResponse> metricResponse =
              getRequestBuilderWithAuthHeader(target).post(entity(testNodeData, APPLICATION_JSON),
                  new GenericType<RestResponse<VerificationNodeDataSetupResponse>>() {});

          assertEquals(0, metricResponse.getResponseMessages().size());
          assertTrue(metricResponse.getResource().isProviderReachable());
          assertTrue(metricResponse.getResource().getLoadResponse().isLoadPresent());
          assertNotNull(metricResponse.getResource().getLoadResponse().getLoadResponse());

          final List<AppdynamicsMetric> tierMetrics =
              (List<AppdynamicsMetric>) metricResponse.getResource().getLoadResponse().getLoadResponse();
          assertFalse(tierMetrics.isEmpty());

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
        }
      }
    }
  }
}
