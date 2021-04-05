package io.harness.delegate.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cli.CliResponse;
import io.harness.delegate.task.terraform.TerraformBaseHelperImpl;
import io.harness.logging.LogCallback;
import io.harness.logging.PlanJsonLogOutputStream;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.terraform.TerraformClientImpl;
import io.harness.terraform.request.TerraformApplyCommandRequest;
import io.harness.terraform.request.TerraformDestroyCommandRequest;
import io.harness.terraform.request.TerraformExecuteStepRequest;
import io.harness.terraform.request.TerraformExecuteStepRequest.TerraformExecuteStepRequestBuilder;
import io.harness.terraform.request.TerraformInitCommandRequest;
import io.harness.terraform.request.TerraformPlanCommandRequest;
import io.harness.terraform.request.TerraformRefreshCommandRequest;

import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class TerraformBaseHelperImplTest extends CategoryTest {
  @InjectMocks @Inject TerraformBaseHelperImpl terraformBaseHelper;
  @Mock private LogCallback logCallback;
  @Mock private PlanJsonLogOutputStream planJsonLogOutputStream;
  @Mock private TerraformClientImpl terraformClient;
  private TerraformBaseHelperImpl spyTerraformBaseHelper;

  private final EncryptedRecordData encryptedPlanContent =
      EncryptedRecordData.builder().name("planName").encryptedValue("encryptedPlan".toCharArray()).build();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    spyTerraformBaseHelper = spy(terraformBaseHelper);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testParseOutput() {
    String workspaceCommandOutput = "* w1\n w2\n w3";
    assertThat(Arrays.asList("w1", "w2", "w3").equals(terraformBaseHelper.parseOutput(workspaceCommandOutput)))
        .isTrue();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testexecuteTerraformApplyStep() throws InterruptedException, TimeoutException, IOException {
    TerraformExecuteStepRequest terraformExecuteStepRequest = getTerraformExecuteStepRequest().build();

    doReturn(Arrays.asList("w1")).when(spyTerraformBaseHelper).parseOutput("* w1\n");

    when(terraformClient.getWorkspaceList(terraformExecuteStepRequest.getEnvVars(),
             terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback()))
        .thenReturn(CliResponse.builder().output("workspace").build());

    spyTerraformBaseHelper.executeTerraformApplyStep(terraformExecuteStepRequest);

    Mockito.verify(terraformClient, times(1))
        .init(TerraformInitCommandRequest.builder()
                  .tfBackendConfigsFilePath(terraformExecuteStepRequest.getTfBackendConfigsFile().getAbsolutePath())
                  .build(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .getWorkspaceList(terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .workspace(terraformExecuteStepRequest.getWorkspace(), true, terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .apply(TerraformApplyCommandRequest.builder().planName("tfplan").build(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .plan(TerraformPlanCommandRequest.builder().build(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testexecuteTerraformPlanStep() throws InterruptedException, TimeoutException, IOException {
    TerraformExecuteStepRequest terraformExecuteStepRequest = getTerraformExecuteStepRequest().build();

    doReturn(Arrays.asList("w1")).when(spyTerraformBaseHelper).parseOutput("* w1\n");

    when(terraformClient.getWorkspaceList(terraformExecuteStepRequest.getEnvVars(),
             terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback()))
        .thenReturn(CliResponse.builder().output("workspace").build());

    terraformBaseHelper.executeTerraformPlanStep(terraformExecuteStepRequest);

    Mockito.verify(terraformClient, times(1))
        .init(TerraformInitCommandRequest.builder()
                  .tfBackendConfigsFilePath(terraformExecuteStepRequest.getTfBackendConfigsFile().getAbsolutePath())
                  .build(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());

    Mockito.verify(terraformClient, times(1))
        .getWorkspaceList(terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());

    Mockito.verify(terraformClient, times(1))
        .workspace(terraformExecuteStepRequest.getWorkspace(), true, terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());

    Mockito.verify(terraformClient, times(1))
        .refresh(TerraformRefreshCommandRequest.builder().build(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());

    Mockito.verify(terraformClient, times(1))
        .plan(TerraformPlanCommandRequest.builder().build(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testexecuteTerraformDestroyStep() throws InterruptedException, TimeoutException, IOException {
    TerraformExecuteStepRequest terraformExecuteStepRequest = getTerraformExecuteStepRequest().build();
    doReturn(Arrays.asList("w1")).when(spyTerraformBaseHelper).parseOutput("* w1\n");

    when(terraformClient.getWorkspaceList(terraformExecuteStepRequest.getEnvVars(),
             terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback()))
        .thenReturn(CliResponse.builder().output("workspace").build());

    terraformBaseHelper.executeTerraformDestroyStep(terraformExecuteStepRequest);

    Mockito.verify(terraformClient, times(1))
        .init(TerraformInitCommandRequest.builder()
                  .tfBackendConfigsFilePath(terraformExecuteStepRequest.getTfBackendConfigsFile().getAbsolutePath())
                  .build(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .getWorkspaceList(terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .workspace(terraformExecuteStepRequest.getWorkspace(), true, terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .destroy(TerraformDestroyCommandRequest.builder().targets(terraformExecuteStepRequest.getTargets()).build(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());
  }

  private TerraformExecuteStepRequestBuilder getTerraformExecuteStepRequest() {
    return TerraformExecuteStepRequest.builder()
        .tfBackendConfigsFile(new File("backendConfigFile"))
        .tfOutputsFile(new File("outputfile"))
        .scriptDirectory("scriptDirectory")
        .envVars(new HashMap<>())
        .workspace("workspace")
        .isRunPlanOnly(false)
        .isSkipRefreshBeforeApplyingPlan(true)
        .isSaveTerraformJson(false)
        .logCallback(logCallback)
        .planJsonLogOutputStream(planJsonLogOutputStream);
  }
}
