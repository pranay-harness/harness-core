package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.states.APMVerificationState.URL_BODY_APPENDER;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.STATE_EXECUTION_ID;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Injector;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.serializer.YamlUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.APMVerificationState.Method;
import software.wings.sm.states.APMVerificationState.MetricCollectionInfo;
import software.wings.sm.states.APMVerificationState.ResponseMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class APMVerificationStateTest extends WingsBaseTest {
  @Inject private Injector injector;
  @Mock private WorkflowStandardParams workflowStandardParams;

  private ExecutionContextImpl context;

  /**
   * Sets context.
   */
  @Before
  public void setupContext() {
    StateExecutionInstance stateExecutionInstance =
        aStateExecutionInstance().displayName("healthCheck1").uuid(STATE_EXECUTION_ID).build();
    when(workflowStandardParams.getApp()).thenReturn(anApplication().uuid(APP_ID).name(APP_NAME).build());
    when(workflowStandardParams.getEnv())
        .thenReturn(anEnvironment().uuid(ENV_ID).name(ENV_NAME).environmentType(EnvironmentType.NON_PROD).build());
    when(workflowStandardParams.getAppId()).thenReturn(APP_ID);
    when(workflowStandardParams.getEnvId()).thenReturn(ENV_ID);

    when(workflowStandardParams.getElementType()).thenReturn(ContextElementType.STANDARD);
    context = new ExecutionContextImpl(stateExecutionInstance, null, injector);
    context.pushContextElement(workflowStandardParams);
    context.pushContextElement(aHostElement().withHostName("localhost").build());
  }

  @Test
  @Category(UnitTests.class)
  public void metricCollectionInfos() throws IOException {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    YamlUtils yamlUtils = new YamlUtils();
    String yamlStr =
        Resources.toString(APMVerificationStateTest.class.getResource("/apm/apm_config.yml"), Charsets.UTF_8);
    List<APMVerificationState.MetricCollectionInfo> mcInfo =
        yamlUtils.read(yamlStr, new TypeReference<List<APMVerificationState.MetricCollectionInfo>>() {});
    apmVerificationState.setMetricCollectionInfos(mcInfo);
    Map<String, List<APMMetricInfo>> apmMetricInfos = apmVerificationState.apmMetricInfos(context);
    assertEquals(3, apmMetricInfos.size());
    assertEquals(2, apmMetricInfos.get("query").size());
    assertNotNull(apmMetricInfos.get("query").get(0).getResponseMappers().get("txnName").getFieldValue());
    assertNotNull(apmMetricInfos.get("query").get(1).getResponseMappers().get("txnName").getJsonPath());
    String body = "this is a dummy collection body";
    assertEquals("One metric with body", 1, apmMetricInfos.get("queryWithHost" + URL_BODY_APPENDER + body).size());
    assertEquals("Body should be present", body,
        apmMetricInfos.get("queryWithHost" + URL_BODY_APPENDER + body).get(0).getBody());
    assertEquals("Method should be post", Method.POST,
        apmMetricInfos.get("queryWithHost" + URL_BODY_APPENDER + body).get(0).getMethod());

    assertEquals("There should be one query with host", 1, apmMetricInfos.get("queryWithHost").size());
    APMMetricInfo metricWithHost = apmMetricInfos.get("queryWithHost").get(0);
    assertNotNull("Query with host has a hostJson", metricWithHost.getResponseMappers().get("host").getJsonPath());
    assertNotNull("Query with host has a hostRegex", metricWithHost.getResponseMappers().get("host").getRegexs());
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateFields() {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    apmVerificationState.setMetricCollectionInfos(null);
    Map<String, String> invalidFields = apmVerificationState.validateFields();
    assertTrue("Size should be 1", invalidFields.size() == 1);
    assertEquals(
        "Metric Collection Info should be missing", "Metric Collection Info", invalidFields.keySet().iterator().next());
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateFieldsResponseMapping() {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    MetricCollectionInfo info =
        MetricCollectionInfo.builder().collectionUrl("This is a sample URL").metricName("name").build();
    apmVerificationState.setMetricCollectionInfos(Arrays.asList(info));
    Map<String, String> invalidFields = apmVerificationState.validateFields();
    assertTrue("Size should be 1", invalidFields.size() == 1);
    assertEquals("ResponseMapping should be missing", "responseMapping", invalidFields.keySet().iterator().next());
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateFieldsResponseMappingMetricValue() {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    MetricCollectionInfo info =
        MetricCollectionInfo.builder().collectionUrl("This is a sample URL").metricName("name").build();
    ResponseMapping mapping =
        ResponseMapping.builder().metricValueJsonPath("metricValue").timestampJsonPath("timestamp").build();
    info.setResponseMapping(mapping);
    apmVerificationState.setMetricCollectionInfos(Arrays.asList(info));
    Map<String, String> invalidFields = apmVerificationState.validateFields();
    assertTrue("Size should be 1", invalidFields.size() == 1);
    assertEquals("transactionName should be missing", "transactionName", invalidFields.keySet().iterator().next());
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateFieldsResponseMappingHostName() {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    MetricCollectionInfo info =
        MetricCollectionInfo.builder().collectionUrl("This is a sample URL ${host}").metricName("name").build();
    ResponseMapping mapping = ResponseMapping.builder()
                                  .metricValueJsonPath("metricValue")
                                  .timestampJsonPath("timestamp")
                                  .txnNameFieldValue("txnName")
                                  .build();
    info.setResponseMapping(mapping);
    apmVerificationState.setMetricCollectionInfos(Arrays.asList(info));
    Map<String, String> invalidFields = apmVerificationState.validateFields();
    assertTrue("Size should be 0", invalidFields.size() == 0);
  }
}
