package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import io.harness.serializer.YamlUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.CustomLogVerificationState.LogCollectionInfo;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CustomLogVerificationStateTest extends WingsBaseTest {
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
  public void testConstructLogDefinitions() throws IOException {
    CustomLogVerificationState state = new CustomLogVerificationState("dummy");
    YamlUtils yamlUtils = new YamlUtils();
    String yamlStr =
        Resources.toString(CustomLogVerificationStateTest.class.getResource("/apm/log_config.yml"), Charsets.UTF_8);
    List<LogCollectionInfo> collectionInfos = yamlUtils.read(yamlStr, new TypeReference<List<LogCollectionInfo>>() {});
    state.setLogCollectionInfos(collectionInfos);

    Map<String, Map<String, ResponseMapper>> logDefinitions = state.constructLogDefinitions(context);
    assertNotNull(logDefinitions);
    assertTrue("Correct query exists", logDefinitions.containsKey("customLogVerificationQuery"));
    Map<String, ResponseMapper> mapping = logDefinitions.get("customLogVerificationQuery");
    assertEquals("Host json path is correct", "hits.hits[*]._source.kubernetes.pod.name",
        mapping.get("host").getJsonPath().get(0));
    assertEquals("timestamp json path is correct", "hits.hits[*]._source.@timestamp",
        mapping.get("timestamp").getJsonPath().get(0));
    assertEquals(
        "logMessage json path is correct", "hits.hits[*]._source.log", mapping.get("logMessage").getJsonPath().get(0));
  }
}
