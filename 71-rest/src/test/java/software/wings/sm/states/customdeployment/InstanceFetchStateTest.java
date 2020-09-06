package software.wings.sm.states.customdeployment;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.SweepingOutputInstance.Scope.WORKFLOW;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.sm.states.customdeployment.InstanceFetchState.OUTPUT_PATH_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.SweepingOutputInstance;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskData.TaskDataKeys;
import io.harness.rule.Owner;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.ResponseData;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.InfraMappingElement;
import software.wings.api.InfraMappingElement.Custom;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.customdeployment.InstanceFetchStateExecutionData;
import software.wings.api.shellscript.provision.ShellScriptProvisionExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.CustomInfrastructureMapping;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.shellscript.provisioner.ShellScriptProvisionParameters;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;
import software.wings.service.impl.ActivityHelperService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionContext;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InstanceFetchStateTest extends WingsBaseTest {
  @Mock private ExecutionContext context;
  @Mock private ActivityHelperService activityHelperService;
  @Mock private CustomDeploymentTypeService customDeploymentTypeService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private DelegateService delegateService;
  @Mock private SweepingOutputService sweepingOutputService;

  @InjectMocks private InstanceFetchState state = new InstanceFetchState("Fetch Instances");

  private static String resourcePath = "./software/wings/customdeployment";

  @Before
  public void setUp() throws Exception {
    String timeoutExpr = "${workflow.variables.timeout}";
    state.setStateTimeoutInMinutes(timeoutExpr);

    final CustomInfrastructureMapping infraMapping = CustomInfrastructureMapping.builder().build();
    infraMapping.setCustomDeploymentTemplateId(TEMPLATE_ID);
    infraMapping.setDeploymentTypeTemplateVersion("1");

    doReturn(ACCOUNT_ID).when(context).getAccountId();
    doReturn(APP_ID).when(context).getAppId();
    doReturn(INFRA_MAPPING_ID).when(context).fetchInfraMappingId();
    doReturn(
        InfraMappingElement.builder().custom(Custom.builder().vars(ImmutableMap.of("key", "value")).build()).build())
        .when(context)
        .fetchInfraMappingElement();
    doReturn(infraMapping).when(infrastructureMappingService).get(APP_ID, INFRA_MAPPING_ID);
    doReturn(CustomDeploymentTypeTemplate.builder()
                 .fetchInstanceScript("echo Hello")
                 .hostAttributes(ImmutableMap.of("key", "value"))
                 .build())
        .when(customDeploymentTypeService)
        .fetchDeploymentTemplate(ACCOUNT_ID, TEMPLATE_ID, "1");
    doReturn(Activity.builder().uuid(ACTIVITY_ID).build())
        .when(activityHelperService)
        .createAndSaveActivity(eq(context), eq(Activity.Type.Command), anyString(),
            eq(CommandUnitType.CUSTOM_DEPLOYMENT_FETCH_INSTANCES.getName()), anyList());
    doReturn("some-string").when(context).appendStateExecutionId(anyString());
    doReturn(SweepingOutputInstance.builder()).when(context).prepareSweepingOutputBuilder(WORKFLOW);
    doAnswer(invocation -> invocation.getArgumentAt(0, String.class)).when(context).renderExpression(anyString());
    doAnswer(invocation -> invocation.getArgumentAt(0, String.class))
        .when(context)
        .renderExpression(anyString(), any(StateExecutionContext.class));
    doReturn("5").when(context).renderExpression(timeoutExpr);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void execute() {
    state.setTags(Arrays.asList("tag1", null, " tag2 ", "tag1 ", "", null));
    final ExecutionResponse response = state.execute(context);

    final ShellScriptProvisionParameters expectedTaskParams =
        ShellScriptProvisionParameters.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .scriptBody("echo Hello")
            .textVariables(ImmutableMap.of("key", "value"))
            .commandUnit(CommandUnitType.CUSTOM_DEPLOYMENT_FETCH_INSTANCES.getName())
            .outputPathKey(OUTPUT_PATH_KEY)
            .workflowExecutionId(context.getWorkflowExecutionId())
            .build();

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    final DelegateTask expected = DelegateTask.builder()
                                      .accountId(ACCOUNT_ID)
                                      .description("Fetch Instances")
                                      .waitId(ACTIVITY_ID)
                                      .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
                                      .tags(Arrays.asList("tag1", "tag2"))
                                      .data(TaskData.builder()
                                                .async(true)
                                                .parameters(new Object[] {expectedTaskParams})
                                                .taskType(TaskType.SHELL_SCRIPT_PROVISION_TASK.name())
                                                .timeout(5 * 60 * 1000)
                                                .build())
                                      .build();
    verify(delegateService).queueTask(captor.capture());

    final DelegateTask task = captor.getValue();
    assertThat(task).isEqualToIgnoringGivenFields(expected, DelegateTaskKeys.data, DelegateTaskKeys.validUntil);
    assertThat(task.getData()).isEqualToIgnoringGivenFields(expected.getData(), TaskDataKeys.expressionFunctorToken);
    assertThat(task.getData().getExpressionFunctorToken()).isNotEqualTo(0);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void handleAsyncResponse() {
    doReturn(InstanceFetchStateExecutionData.builder()
                 .activityId(ACTIVITY_ID)
                 .hostObjectArrayPath("Instances")
                 .hostAttributes(ImmutableMap.of("hostname", "ip"))
                 .build())
        .when(context)
        .getStateExecutionData();
    Map<String, ResponseData> response = ImmutableMap.of(ACTIVITY_ID,
        ShellScriptProvisionExecutionData.builder()
            .activityId(ACTIVITY_ID)
            .output("{\"Instances\": [{\"ip\":\"1.1\"},{\"ip\":\"2.2\"}]}")
            .executionStatus(SUCCESS)
            .build());

    final ExecutionResponse executionResponse = state.handleAsyncResponse(context, response);

    verify(activityHelperService, times(1)).updateStatus(ACTIVITY_ID, APP_ID, SUCCESS);

    assertThat(executionResponse.getContextElements()).hasSize(1);
    assertThat(executionResponse.getNotifyElements()).hasSize(1);
    InstanceElementListParam elementListParam =
        (InstanceElementListParam) executionResponse.getContextElements().get(0);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(elementListParam.getInstanceElements()).hasSize(2);
    assertThat(
        elementListParam.getInstanceElements().stream().map(InstanceElement::getHostName).collect(Collectors.toList()))
        .containsExactly("1.1", "2.2");
    assertThat(
        elementListParam.getInstanceElements().stream().map(InstanceElement::getName).collect(Collectors.toList()))
        .containsExactly("1.1", "2.2");
    assertThat(executionResponse.getErrorMessage()).isNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void handleAsyncResponseWithInvalidJson() {
    doReturn(InstanceFetchStateExecutionData.builder()
                 .hostObjectArrayPath("Instances")
                 .hostAttributes(ImmutableMap.of("hostname", "ip"))
                 .build())
        .when(context)
        .getStateExecutionData();
    Map<String, ResponseData> response = ImmutableMap.of(ACTIVITY_ID,
        ShellScriptProvisionExecutionData.builder()
            .activityId(ACTIVITY_ID)
            .output("{\"Instances\": [\"ip\":\"1.1\"},{\"ip\":\"2.2\"}]}")
            .executionStatus(SUCCESS)
            .build());

    final ExecutionResponse executionResponse = state.handleAsyncResponse(context, response);

    assertThat(executionResponse.getContextElements()).isEmpty();
    assertThat(executionResponse.getNotifyElements()).isEmpty();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(FAILED);
    assertThat(executionResponse.getErrorMessage())
        .isEqualTo(
            "Reason: JsonParseException: Unexpected character (':' (code 58)): was expecting comma to separate ARRAY entries\n"
            + " at [Source: {\"Instances\": [\"ip\":\"1.1\"},{\"ip\":\"2.2\"}]}; line: 1, column: 21]");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void handleAsyncResponseFailure() {
    doReturn(InstanceFetchStateExecutionData.builder()
                 .activityId(ACTIVITY_ID)
                 .hostObjectArrayPath("Instances")
                 .hostAttributes(ImmutableMap.of("hostname", "ip"))
                 .build())
        .when(context)
        .getStateExecutionData();
    Map<String, ResponseData> response = ImmutableMap.of(ACTIVITY_ID,
        ShellScriptProvisionExecutionData.builder()
            .activityId(ACTIVITY_ID)
            .errorMsg("invalid script")
            .executionStatus(FAILED)
            .build());

    final ExecutionResponse executionResponse = state.handleAsyncResponse(context, response);

    verify(activityHelperService, times(1)).updateStatus(ACTIVITY_ID, APP_ID, FAILED);

    assertThat(executionResponse.getExecutionStatus()).isEqualTo(FAILED);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("invalid script");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testMapJsonToInstanceElements() throws IOException {
    InstanceFetchStateExecutionData executionData = InstanceFetchStateExecutionData.builder()
                                                        .hostObjectArrayPath("hosts.value")
                                                        .hostAttributes(ImmutableMap.of("hostname", "access_ip_v4"))
                                                        .build();
    String output = readFile("Instances.json");

    final List<InstanceElement> instanceElements =
        InstanceElementMapperUtils.mapJsonToInstanceElements(executionData.getHostAttributes(),
            executionData.getHostObjectArrayPath(), output, InstanceFetchState.instanceElementMapper);

    assertThat(instanceElements).hasSize(2);
    assertThat(instanceElements.stream().map(InstanceElement::getHostName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("10.244.12.15", "10.244.12.13");
  }

  /*
  Test with output of kubectl get pod -l app=nginx -o json
   */
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testMapJsonToInstanceElementsNestedAttributes() throws IOException {
    InstanceFetchStateExecutionData executionData = InstanceFetchStateExecutionData.builder()
                                                        .hostObjectArrayPath("items")
                                                        .hostAttributes(ImmutableMap.of("hostname", "metadata.name"))
                                                        .build();
    String output = readFile("NestedAttributesInstance.json");

    final List<InstanceElement> instanceElements =
        InstanceElementMapperUtils.mapJsonToInstanceElements(executionData.getHostAttributes(),
            executionData.getHostObjectArrayPath(), output, InstanceFetchState.instanceElementMapper);

    assertThat(instanceElements).hasSize(5);
    assertThat(instanceElements.stream().map(InstanceElement::getHostName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder(
            "cd-statefulset-0", "cd-statefulset-1", "k8sv2-statefulset-0", "statefulset-test-0", "statefulset-test-1");
  }

  private String readFile(String fileName) throws IOException {
    File file = null;
    try {
      file = new File(getClass().getClassLoader().getResource(resourcePath + PATH_DELIMITER + fileName).toURI());
    } catch (URISyntaxException e) {
      fail("Unable to find file " + fileName);
    }
    assertThat(file).isNotNull();
    return FileUtils.readFileToString(file, "UTF-8");
  }
}