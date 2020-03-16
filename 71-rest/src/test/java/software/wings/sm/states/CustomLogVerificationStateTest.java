package software.wings.sm.states;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.states.CustomLogVerificationState.constructLogDefinitions;
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
import io.harness.rule.Owner;
import io.harness.serializer.YamlUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.HostElement;
import software.wings.beans.Environment.EnvironmentType;
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
        aStateExecutionInstance().displayName("healthCheck1").uuid(STATE_EXECUTION_ID).build();
    when(workflowStandardParams.getApp()).thenReturn(anApplication().uuid(APP_ID).name(APP_NAME).build());
    when(workflowStandardParams.getEnv())
        .thenReturn(anEnvironment().uuid(ENV_ID).name(ENV_NAME).environmentType(EnvironmentType.NON_PROD).build());
    when(workflowStandardParams.getAppId()).thenReturn(APP_ID);
    when(workflowStandardParams.getEnvId()).thenReturn(ENV_ID);

    when(workflowStandardParams.getElementType()).thenReturn(ContextElementType.STANDARD);
    context = new ExecutionContextImpl(stateExecutionInstance, null, injector);
    context.pushContextElement(workflowStandardParams);
    context.pushContextElement(HostElement.builder().hostName("localhost").build());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testConstructLogDefinitions() throws IOException {
    CustomLogVerificationState state = new CustomLogVerificationState("dummy");
    YamlUtils yamlUtils = new YamlUtils();
    String yamlStr =
        Resources.toString(CustomLogVerificationStateTest.class.getResource("/apm/log_config.yml"), Charsets.UTF_8);
    List<LogCollectionInfo> collectionInfos = yamlUtils.read(yamlStr, new TypeReference<List<LogCollectionInfo>>() {});
    state.setLogCollectionInfos(collectionInfos);

    Map<String, Map<String, ResponseMapper>> logDefinitions = constructLogDefinitions(context, collectionInfos);
    assertThat(logDefinitions).isNotNull();
    assertThat(logDefinitions.containsKey("customLogVerificationQuery")).isTrue();
    Map<String, ResponseMapper> mapping = logDefinitions.get("customLogVerificationQuery");
    assertThat("hits.hits[*]._source.kubernetes.pod.name").isEqualTo(mapping.get("host").getJsonPath().get(0));
    assertThat("hits.hits[*]._source.@timestamp").isEqualTo(mapping.get("timestamp").getJsonPath().get(0));
    assertThat("hits.hits[*]._source.log").isEqualTo(mapping.get("logMessage").getJsonPath().get(0));
    assertThat(state.shouldInspectHostsForLogAnalysis()).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testShouldDoHostFiltering() throws IOException {
    CustomLogVerificationState state = new CustomLogVerificationState("dummy");
    YamlUtils yamlUtils = new YamlUtils();
    String yamlStr = Resources.toString(
        CustomLogVerificationStateTest.class.getResource("/apm/log_config_noHost.yml"), Charsets.UTF_8);
    List<LogCollectionInfo> collectionInfos = yamlUtils.read(yamlStr, new TypeReference<List<LogCollectionInfo>>() {});
    state.setLogCollectionInfos(collectionInfos);

    boolean shouldDOHostFiltering = state.shouldInspectHostsForLogAnalysis();
    assertThat(shouldDOHostFiltering).isFalse();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testShouldDoHostFiltering_true() throws IOException {
    CustomLogVerificationState state = new CustomLogVerificationState("dummy");
    YamlUtils yamlUtils = new YamlUtils();
    String yamlStr =
        Resources.toString(CustomLogVerificationStateTest.class.getResource("/apm/log_config.yml"), Charsets.UTF_8);
    List<LogCollectionInfo> collectionInfos = yamlUtils.read(yamlStr, new TypeReference<List<LogCollectionInfo>>() {});
    state.setLogCollectionInfos(collectionInfos);

    boolean shouldDOHostFiltering = state.shouldInspectHostsForLogAnalysis();
    assertThat(shouldDOHostFiltering).isTrue();
  }
}
