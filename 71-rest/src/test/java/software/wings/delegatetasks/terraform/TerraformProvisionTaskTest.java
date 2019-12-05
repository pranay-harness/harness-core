package software.wings.delegatetasks.terraform;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.OwnerRule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.delegatetasks.TerraformProvisionTask;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.WingsTestConstants;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TerraformProvisionTaskTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;

  TerraformProvisionTask terraformProvisionTask = new TerraformProvisionTask(WingsTestConstants.DELEGATE_ID,
      DelegateTask.builder().async(true).data(TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
      delegateTaskResponse -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(terraformProvisionTask).set("encryptionService", mockEncryptionService);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void getTargetsArgsTest() {
    assertThat(terraformProvisionTask.getTargetArgs(null)).isEqualTo("");
    assertThat(terraformProvisionTask.getTargetArgs(Collections.EMPTY_LIST)).isEqualTo("");

    List<String> targets = new ArrayList<>(Arrays.asList("target1", "target2"));

    assertThat(terraformProvisionTask.getTargetArgs(targets)).isEqualTo("-target=target1 -target=target2 ");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testParseOutput() {
    String workspaceCommandOutput = "* w1\n  w2\n w3";
    assertThat(Arrays.asList("w1", "w2", "w3").equals(terraformProvisionTask.parseOutput(workspaceCommandOutput)))
        .isTrue();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGenerateInlineCommandVars() throws Exception {
    doReturn(new char[] {'v', '2'}).when(mockEncryptionService).getDecryptedValue(any());
    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .useVarForInlineVariables(true)
            .variables(ImmutableMap.of("k1", "v1"))
            .encryptedVariables(ImmutableMap.of("k2", EncryptedDataDetail.builder().build()))
            .build();
    StringBuilder inlineCommandBuffer = new StringBuilder();
    StringBuilder inlineUILogBuffer = new StringBuilder();
    terraformProvisionTask.getCommandLineVariableParams(parameters, null, inlineCommandBuffer, inlineUILogBuffer);
    String varParams = inlineCommandBuffer.toString();
    String uiLogs = inlineUILogBuffer.toString();
    assertThat(varParams).isEqualTo(" -var='k1=v1'  -var='k2=v2' ");
    assertThat(uiLogs).isEqualTo(" -var='k1=v1'  -var='k2=HarnessSecret:[k2]' ");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGenerateInlineCommandVarsFile() throws Exception {
    doReturn(new char[] {'v', '2'}).when(mockEncryptionService).getDecryptedValue(any());
    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .useVarForInlineVariables(false)
            .variables(ImmutableMap.of("k1", "v1"))
            .encryptedVariables(ImmutableMap.of("k2", EncryptedDataDetail.builder().build()))
            .build();
    StringBuilder inlineCommandBuffer = new StringBuilder();
    StringBuilder inlineUILogBuffer = new StringBuilder();
    File tempFile = File.createTempFile(format("Temp-[%s]", UUID.randomUUID().toString()), ".tmp");
    terraformProvisionTask.getCommandLineVariableParams(parameters, tempFile, inlineCommandBuffer, inlineUILogBuffer);
    String fileContents = new String(Files.readAllBytes(tempFile.toPath()));
    assertThat(fileContents)
        .isEqualTo("k1 = \"v1\"\n"
            + "k2 = \"v2\"\n");
    tempFile.deleteOnExit();
  }
}