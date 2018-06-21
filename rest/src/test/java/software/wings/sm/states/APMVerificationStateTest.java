package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.YamlUtils;

import java.io.IOException;
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
        aStateExecutionInstance().withDisplayName("healthCheck1").withUuid(STATE_EXECUTION_ID).build();
    when(workflowStandardParams.getApp()).thenReturn(anApplication().withUuid(APP_ID).withName(APP_NAME).build());
    when(workflowStandardParams.getEnv())
        .thenReturn(
            anEnvironment().withUuid(ENV_ID).withName(ENV_NAME).withEnvironmentType(EnvironmentType.NON_PROD).build());
    when(workflowStandardParams.getAppId()).thenReturn(APP_ID);
    when(workflowStandardParams.getEnvId()).thenReturn(ENV_ID);

    when(workflowStandardParams.getElementType()).thenReturn(ContextElementType.STANDARD);
    context = new ExecutionContextImpl(stateExecutionInstance, null, injector);
    context.pushContextElement(workflowStandardParams);
    context.pushContextElement(aHostElement().withHostName("localhost").build());
  }

  @Test
  public void metricCollectionInfos() throws IOException {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    YamlUtils yamlUtils = new YamlUtils();
    String yamlStr =
        Resources.toString(APMVerificationStateTest.class.getResource("/apm/apm_config.yml"), Charsets.UTF_8);
    List<APMVerificationState.MetricCollectionInfo> mcInfo =
        yamlUtils.read(yamlStr, new TypeReference<List<APMVerificationState.MetricCollectionInfo>>() {});
    apmVerificationState.setMetricCollectionInfos(mcInfo);
    Map<String, List<APMMetricInfo>> apmMetricInfos = apmVerificationState.apmMetricInfos(context);
    assertEquals(2, apmMetricInfos.size());
    assertEquals(2, apmMetricInfos.get("query").size());
    assertNotNull(apmMetricInfos.get("query").get(0).getResponseMappers().get("txnName").getFieldValue());
    assertNotNull(apmMetricInfos.get("query").get(1).getResponseMappers().get("txnName").getJsonPath());

    assertEquals("There should be one query with host", 1, apmMetricInfos.get("queryWithHost").size());
    APMMetricInfo metricWithHost = apmMetricInfos.get("queryWithHost").get(0);
    assertNotNull("Query with host has a hostJson", metricWithHost.getResponseMappers().get("host").getJsonPath());
    assertNotNull("Query with host has a hostRegex", metricWithHost.getResponseMappers().get("host").getRegexs());
  }
}
