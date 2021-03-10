package io.harness.terraform;

import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.terraform.TerraformConstants.DEFAULT_TERRAFORM_COMMAND_TIMEOUT;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cli.CliHelper;
import io.harness.cli.CliResponse;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.terraform.request.TerraformApplyCommandRequest;
import io.harness.terraform.request.TerraformDestroyCommandRequest;
import io.harness.terraform.request.TerraformInitCommandRequest;
import io.harness.terraform.request.TerraformPlanCommandRequest;
import io.harness.terraform.request.TerraformRefreshCommandRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TerraformClientImplTest extends CategoryTest {
  @Mock private CliHelper cliHelper;
  @Mock LogCallback logCallback;
  @InjectMocks private TerraformClientImpl terraformClientImpl;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testInitCommand() throws InterruptedException, IOException, TimeoutException {
    CliResponse cliResponse = CliResponse.builder().build();
    TerraformInitCommandRequest terraformInitCommandRequest =
        TerraformInitCommandRequest.builder().tfBackendConfigsFilePath("/tmp/terraform/backendconfig.tf").build();

    doReturn(cliResponse)
        .when(cliHelper)
        .executeCliCommand(format("terraform init -input=false -backend-config=%s",
                               terraformInitCommandRequest.getTfBackendConfigsFilePath()),
            DEFAULT_TERRAFORM_COMMAND_TIMEOUT, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    CliResponse actualResponse = terraformClientImpl.init(
        terraformInitCommandRequest, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testDestroyCommand() throws InterruptedException, IOException, TimeoutException {
    CliResponse cliResponse = CliResponse.builder().build();
    TerraformDestroyCommandRequest terraformDestroyCommandRequest =
        TerraformDestroyCommandRequest.builder()
            .targets(Arrays.asList("10.0.10.1", "10.0.10.2"))
            .varFilePaths(Arrays.asList("variableParams"))
            .build();
    doReturn(cliResponse)
        .when(cliHelper)
        .executeCliCommand(format("terraform destroy -force %s %s",
                               TerraformHelperUtils.generateCommandFlagsString(
                                   terraformDestroyCommandRequest.getTargets(), "-target="),
                               TerraformHelperUtils.generateCommandFlagsString(
                                   terraformDestroyCommandRequest.getVarFilePaths(), "-var-file=")),
            DEFAULT_TERRAFORM_COMMAND_TIMEOUT, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    CliResponse actualResponse = terraformClientImpl.destroy(
        terraformDestroyCommandRequest, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testPlanCommand() throws InterruptedException, IOException, TimeoutException {
    CliResponse cliResponse = CliResponse.builder().build();
    TerraformPlanCommandRequest terraformPlanCommandRequest =
        TerraformPlanCommandRequest.builder().destroySet(true).build();

    doReturn(cliResponse)
        .when(cliHelper)
        .executeCliCommand(
            format("terraform plan -input=false -destroy -out=tfdestroyplan %s %s",
                TerraformHelperUtils.generateCommandFlagsString(terraformPlanCommandRequest.getTargets(), "-target="),
                TerraformHelperUtils.generateCommandFlagsString(
                    terraformPlanCommandRequest.getVarFilePaths(), "-var-file=")),
            DEFAULT_TERRAFORM_COMMAND_TIMEOUT, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    CliResponse actualResponse = terraformClientImpl.plan(
        terraformPlanCommandRequest, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testRefreshCommand() throws InterruptedException, IOException, TimeoutException {
    CliResponse cliResponse = CliResponse.builder().build();
    TerraformRefreshCommandRequest terraformRefreshCommandRequest = TerraformRefreshCommandRequest.builder().build();
    doReturn(cliResponse)
        .when(cliHelper)
        .executeCliCommand("terraform refresh -input=false "
                + TerraformHelperUtils.generateCommandFlagsString(
                    terraformRefreshCommandRequest.getTargets(), "-target=")
                + TerraformHelperUtils.generateCommandFlagsString(
                    terraformRefreshCommandRequest.getVarFilePaths(), "-var-file="),
            DEFAULT_TERRAFORM_COMMAND_TIMEOUT, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    CliResponse actualResponse = terraformClientImpl.refresh(
        terraformRefreshCommandRequest, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testApplyCommand() throws InterruptedException, IOException, TimeoutException {
    CliResponse cliResponse = CliResponse.builder().build();
    TerraformApplyCommandRequest terraformApplyCommandRequest = TerraformApplyCommandRequest.builder().build();
    doReturn(cliResponse)
        .when(cliHelper)
        .executeCliCommand("terraform apply -input=false tfplan", DEFAULT_TERRAFORM_COMMAND_TIMEOUT,
            Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    CliResponse actualResponse = terraformClientImpl.apply(
        terraformApplyCommandRequest, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testWorkspaceCommand() throws InterruptedException, IOException, TimeoutException {
    CliResponse cliResponse = CliResponse.builder().build();
    String workspace = "workspace";
    doReturn(cliResponse)
        .when(cliHelper)
        .executeCliCommand("terraform workspace NEW " + workspace, DEFAULT_TERRAFORM_COMMAND_TIMEOUT,
            Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    CliResponse actualResponse =
        terraformClientImpl.workspace(workspace, true, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testshowCommand() throws InterruptedException, IOException, TimeoutException {
    CliResponse cliResponse = CliResponse.builder().build();
    String plan = "planName";
    doReturn(cliResponse)
        .when(cliHelper)
        .executeCliCommand("terraform show -json " + plan, DEFAULT_TERRAFORM_COMMAND_TIMEOUT, Collections.emptyMap(),
            "SCRIPT_FILES_DIRECTORY", logCallback);

    CliResponse actualResponse =
        terraformClientImpl.show(plan, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testOutputCommand() throws InterruptedException, IOException, TimeoutException {
    CliResponse cliResponse = CliResponse.builder().build();
    String tfOutputsFile = "OutFile.txt";
    doReturn(cliResponse)
        .when(cliHelper)
        .executeCliCommand("terraform show -json > " + tfOutputsFile, DEFAULT_TERRAFORM_COMMAND_TIMEOUT,
            Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    CliResponse actualResponse =
        terraformClientImpl.output(tfOutputsFile, Collections.emptyMap(), "SCRIPT_FILES_DIRECTORY", logCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }
}