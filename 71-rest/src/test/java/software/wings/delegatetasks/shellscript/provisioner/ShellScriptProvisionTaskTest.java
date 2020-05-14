package software.wings.delegatetasks.shellscript.provisioner;

import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.beans.shellscript.provisioner.ShellScriptProvisionParameters;
import software.wings.core.local.executors.ShellExecutorConfig;
import software.wings.core.local.executors.ShellExecutorFactory;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ShellScriptProvisionTaskTest extends WingsBaseTest {
  @Mock private EncryptionService encryptionService;
  @Mock private ShellExecutorFactory shellExecutorFactory;
  @Mock private DelegateLogService logService;

  @InjectMocks
  private ShellScriptProvisionTask shellScriptProvisionTask =
      (ShellScriptProvisionTask) TaskType.SHELL_SCRIPT_PROVISION_TASK.getDelegateRunnableTask("delegateid",
          DelegateTask.builder().data(TaskData.builder().build()).build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {}

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetCombinedVariablesMap() throws IOException {
    assertThat(shellScriptProvisionTask.getCombinedVariablesMap(null, null)).isEmpty();
    assertThat(shellScriptProvisionTask.getCombinedVariablesMap(Collections.emptyMap(), Collections.emptyMap()))
        .isEmpty();

    Map<String, String> textVariables = new HashMap<>();
    textVariables.put("var1", "val1");
    assertThat(shellScriptProvisionTask.getCombinedVariablesMap(textVariables, Collections.emptyMap()))
        .isEqualTo(textVariables);

    Map<String, EncryptedDataDetail> encryptedVariables = new HashMap<>();
    encryptedVariables.put("var2", EncryptedDataDetail.builder().build());

    Mockito.when(encryptionService.getDecryptedValue(any())).thenReturn(new char[] {'a', 'b'});
    Map<String, String> expectedCombinedMap = new HashMap<>();
    expectedCombinedMap.put("var1", "val1");
    expectedCombinedMap.put("var2", "ab");

    assertThat(shellScriptProvisionTask.getCombinedVariablesMap(textVariables, encryptedVariables))
        .isEqualTo(expectedCombinedMap);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void shouldUseWorkflowExecutionIdAsOutputPath() {
    ShellScriptProvisionParameters taskParameters = ShellScriptProvisionParameters.builder()
                                                        .accountId("account-id")
                                                        .workflowExecutionId("workflow-execution-id")
                                                        .build();
    ArgumentCaptor<ShellExecutorConfig> shellExecutorConfigArgumentCaptor =
        ArgumentCaptor.forClass(ShellExecutorConfig.class);

    shellScriptProvisionTask.run(taskParameters);

    verify(shellExecutorFactory).getExecutor(shellExecutorConfigArgumentCaptor.capture());
    assertThat(shellExecutorConfigArgumentCaptor.getValue().getEnvironment().values())
        .anyMatch(item -> item.endsWith("workflow-execution-id/output.json"));
  }
}
