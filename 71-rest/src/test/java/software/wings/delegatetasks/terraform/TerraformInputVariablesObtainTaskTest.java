package software.wings.delegatetasks.terraform;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.mockito.Matchers.any;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.GitOperationContext;
import software.wings.beans.NameValuePair;
import software.wings.beans.TaskType;
import software.wings.beans.TerraformInputVariablesTaskResponse;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.delegatetasks.TerraformInputVariablesObtainTask;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.TerraformConfigInspectService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.GitUtilsDelegate;
import software.wings.utils.WingsTestConstants;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileUtils.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class TerraformInputVariablesObtainTaskTest extends WingsBaseTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock GitService gitService;
  @Mock GitUtilsDelegate gitUtilsDelegate;
  @Mock EncryptionService encryptionService;
  @Mock TerraformConfigInspectService terraformConfigInspectService;

  private TerraformProvisionParameters parameters;

  @InjectMocks
  TerraformInputVariablesObtainTask delegateRunnableTask =
      (TerraformInputVariablesObtainTask) TaskType.TERRAFORM_INPUT_VARIABLES_OBTAIN_TASK.getDelegateRunnableTask(
          WingsTestConstants.DELEGATE_ID,
          DelegateTask.builder()
              .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
              .build(),
          notifyResponseData -> {}, () -> true);

  @Before
  public void setup() {
    initMocks(this);
    parameters = TerraformProvisionParameters.builder()
                     .sourceRepo(GitConfig.builder().branch("master").build())
                     .scriptPath("")
                     .build();
    when(encryptionService.decrypt(any(), any())).thenReturn(null);
    PowerMockito.mockStatic(FileUtils.class);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testRun() throws IOException {
    String moduleDir = "some-dir";
    when(gitUtilsDelegate.cloneRepo(any(), any(), any())).thenReturn(GitOperationContext.builder().build());
    when(gitUtilsDelegate.resolveAbsoluteFilePath(any(), any())).thenReturn(moduleDir);
    PowerMockito.when(FileUtils.listFiles(any(), any(), any())).thenReturn(Arrays.asList(new File(moduleDir)));

    when(terraformConfigInspectService.parseFieldsUnderCategory(moduleDir, "variables"))
        .thenReturn(Arrays.asList("var_1", "var_2"));

    TerraformInputVariablesTaskResponse inputVariables = delegateRunnableTask.run(new Object[] {parameters});
    Assertions.assertThat(inputVariables.getVariablesList().isEmpty()).isFalse();
    List<String> variableNames =
        inputVariables.getVariablesList().stream().map(NameValuePair::getName).collect(Collectors.toList());
    Assertions.assertThat(variableNames).containsExactlyInAnyOrder("var_1", "var_2");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testNoTerraformFilesFound() {
    when(gitUtilsDelegate.cloneRepo(any(), any(), any())).thenReturn(GitOperationContext.builder().build());
    when(gitUtilsDelegate.resolveAbsoluteFilePath(any(), any())).thenReturn("some-path");
    when(FileUtils.listFiles(any(), any(), any())).thenReturn(Collections.EMPTY_LIST);
    TerraformInputVariablesTaskResponse response = delegateRunnableTask.run(new Object[] {parameters});
    Assertions.assertThat(response.getTerraformExecutionData().getErrorMessage())
        .contains("No "
            + "Terraform Files Found");
  }
}
