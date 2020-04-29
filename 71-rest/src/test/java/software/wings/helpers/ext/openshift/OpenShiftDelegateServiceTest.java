package software.wings.helpers.ext.openshift;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cli.CliResponse;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class OpenShiftDelegateServiceTest extends WingsBaseTest {
  @Mock private OpenShiftClient openShiftClient;
  @InjectMocks @Inject private OpenShiftDelegateService openShiftDelegateService;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testProcessTemplatization() throws IOException {
    shouldGiveK8SReadyYaml();
    shouldThrowExceptionWhenOcProcessFails();
    shouldThrowExceptionWhenProcessResultEmpty();
    shouldThrowExceptionWhenItemsEmpty();
  }

  private void shouldThrowExceptionWhenItemsEmpty() throws IOException {
    String OC_BINARY_PATH = "OC_BINARY_PATH";
    String TEMPLATE_FILE_PATH = "TEMPLATE_FILE_PATH";
    String MANIFEST_DIRECTORY_PATH = ".";
    List<String> paramFileContent = Arrays.asList("a:b", "c:d");
    List<String> paramFilePaths = Arrays.asList("params-0", "params-1");
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    File ocResultYamlFile =
        new File(getClass().getClassLoader().getResource("./openshift/oc_empty_items.yaml").getFile());

    CliResponse cliResponse = CliResponse.builder()
                                  .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                  .output(FileUtils.readFileToString(ocResultYamlFile, "UTF-8"))
                                  .build();

    Mockito.doReturn(cliResponse)
        .when(openShiftClient)
        .process(OC_BINARY_PATH, TEMPLATE_FILE_PATH, paramFilePaths, MANIFEST_DIRECTORY_PATH, executionLogCallback);

    assertThatThrownBy(()
                           -> openShiftDelegateService.processTemplatization(MANIFEST_DIRECTORY_PATH, OC_BINARY_PATH,
                               TEMPLATE_FILE_PATH, executionLogCallback, paramFileContent))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Items list can't be empty");
  }

  private void shouldThrowExceptionWhenProcessResultEmpty() {
    String OC_BINARY_PATH = "OC_BINARY_PATH";
    String TEMPLATE_FILE_PATH = "TEMPLATE_FILE_PATH";
    String MANIFEST_DIRECTORY_PATH = ".";
    List<String> paramFileContent = Arrays.asList("a:b", "c:d");
    List<String> paramFilePaths = Arrays.asList("params-0", "params-1");
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    CliResponse cliResponse =
        CliResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).output("").build();

    Mockito.doReturn(cliResponse)
        .when(openShiftClient)
        .process(OC_BINARY_PATH, TEMPLATE_FILE_PATH, paramFilePaths, MANIFEST_DIRECTORY_PATH, executionLogCallback);

    assertThatThrownBy(()
                           -> openShiftDelegateService.processTemplatization(MANIFEST_DIRECTORY_PATH, OC_BINARY_PATH,
                               TEMPLATE_FILE_PATH, executionLogCallback, paramFileContent))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Oc process result can't be empty");
  }

  private void shouldThrowExceptionWhenOcProcessFails() throws IOException {
    String OC_BINARY_PATH = "OC_BINARY_PATH";
    String TEMPLATE_FILE_PATH = "TEMPLATE_FILE_PATH";
    String MANIFEST_DIRECTORY_PATH = ".";
    List<String> paramFileContent = Arrays.asList("a:b", "c:d");
    List<String> paramFilePaths = Arrays.asList("params-0", "params-1");
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    CliResponse cliResponse = CliResponse.builder()
                                  .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                  .output("Invalid parameter")
                                  .build();

    Mockito.doReturn(cliResponse)
        .when(openShiftClient)
        .process(OC_BINARY_PATH, TEMPLATE_FILE_PATH, paramFilePaths, MANIFEST_DIRECTORY_PATH, executionLogCallback);

    assertThatThrownBy(()
                           -> openShiftDelegateService.processTemplatization(MANIFEST_DIRECTORY_PATH, OC_BINARY_PATH,
                               TEMPLATE_FILE_PATH, executionLogCallback, paramFileContent))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Oc process command failed. Invalid parameter");
  }

  private void shouldGiveK8SReadyYaml() throws IOException {
    String OC_BINARY_PATH = "OC_BINARY_PATH";
    String TEMPLATE_FILE_PATH = "TEMPLATE_FILE_PATH";
    String MANIFEST_DIRECTORY_PATH = ".";
    List<String> paramFileContent = Arrays.asList("a:b", "c:d");
    List<String> paramFilePaths = Arrays.asList("params-0", "params-1");
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    File ocResultYamlFile =
        new File(getClass().getClassLoader().getResource("./openshift/oc_process_result.yaml").getFile());
    File expectedYamlFile =
        new File(getClass().getClassLoader().getResource("./openshift/expected_parsed_result.yaml").getFile());

    CliResponse cliResponse = CliResponse.builder()
                                  .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                  .output(FileUtils.readFileToString(ocResultYamlFile, "UTF-8"))
                                  .build();

    Mockito.doReturn(cliResponse)
        .when(openShiftClient)
        .process(OC_BINARY_PATH, TEMPLATE_FILE_PATH, paramFilePaths, MANIFEST_DIRECTORY_PATH, executionLogCallback);

    List<ManifestFile> manifestFiles = openShiftDelegateService.processTemplatization(
        MANIFEST_DIRECTORY_PATH, OC_BINARY_PATH, TEMPLATE_FILE_PATH, executionLogCallback, paramFileContent);

    assertThat(manifestFiles).hasSize(1);
    assertThat(manifestFiles.get(0).getFileContent()).isEqualTo(FileUtils.readFileToString(expectedYamlFile, "UTF-8"));
  }
}