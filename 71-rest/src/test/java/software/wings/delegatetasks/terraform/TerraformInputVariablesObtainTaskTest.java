package software.wings.delegatetasks.terraform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTask.Builder;
import software.wings.beans.GitConfig;
import software.wings.beans.TaskType;
import software.wings.beans.TerraformInputVariablesTaskResponse;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.delegatetasks.TerraformInputVariablesObtainTask;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.WingsTestConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TerraformInputVariablesObtainTaskTest extends WingsBaseTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock GitService gitService;
  @Mock EncryptionService encryptionService;

  @InjectMocks
  TerraformInputVariablesObtainTask delegateRunnableTask =
      (TerraformInputVariablesObtainTask) TaskType.TERRAFORM_INPUT_VARIABLES_OBTAIN_TASK.getDelegateRunnableTask(
          WingsTestConstants.DELEGATE_ID, Builder.aDelegateTask().build(), notifyResponseData -> {}, () -> true);

  @Test
  public void testRun() {
    TerraformProvisionParameters parameters = TerraformProvisionParameters.builder()
                                                  .sourceRepo(GitConfig.builder().branch("master").build())
                                                  .scriptPath("")
                                                  .build();

    List<GitFile> gitFiles = new ArrayList<>();
    gitFiles.add(GitFile.builder()
                     .filePath("main.tf")
                     .fileContent("variable \"access_key\" {}\n"
                         + "variable \"secret_key\" {}\n"
                         + "variable \"region\" {\n"
                         + "  default = \"us-east-1\"\n"
                         + "}\n"
                         + "provider \"aws\" {\n"
                         + "  access_key = \"${var.access_key}\"\n"
                         + "  secret_key = \"${var.secret_key}\"\n"
                         + "  region     = \"us-east-1\"\n"
                         + "}\n"
                         + "\n"
                         + "resource \"aws_instance\" \"example\" {\n"
                         + "  ami           = \"ami-2757f631\"\n"
                         + "  instance_type = \"t2.micro\"\n"
                         + "}")
                     .build());

    when(encryptionService.decrypt(any(), any())).thenReturn(null);
    when(gitService.fetchFilesByPath(any(), any(), any(), any(), any(), anyBoolean()))
        .thenReturn(GitFetchFilesResult.builder().files(gitFiles).build());

    TerraformInputVariablesTaskResponse inputVariables = delegateRunnableTask.run(new Object[] {parameters});
    assertFalse(inputVariables.getVariablesList().isEmpty());

    when(gitService.fetchFilesByPath(any(), any(), any(), any(), any(), anyBoolean()))
        .thenReturn(GitFetchFilesResult.builder().files(Collections.EMPTY_LIST).build());

    inputVariables = delegateRunnableTask.run(new Object[] {parameters});
    assertTrue(inputVariables.getVariablesList().isEmpty());
  }
}
