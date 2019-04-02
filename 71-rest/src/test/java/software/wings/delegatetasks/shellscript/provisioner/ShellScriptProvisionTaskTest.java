package software.wings.delegatetasks.shellscript.provisioner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.core.local.executors.ShellExecutorFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ShellScriptProvisionTaskTest extends WingsBaseTest {
  @Mock private EncryptionService encryptionService;
  @Mock private ShellExecutorFactory shellExecutorFactory;

  @InjectMocks
  private ShellScriptProvisionTask shellScriptProvisionTask =
      (ShellScriptProvisionTask) TaskType.SHELL_SCRIPT_PROVISION_TASK.getDelegateRunnableTask("delegateid",
          DelegateTask.builder().data(TaskData.builder().build()).build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {}

  @Test
  @Category(UnitTests.class)
  public void testGetCombinedVariablesMap() throws IOException {
    assertEquals(Collections.emptyMap(), shellScriptProvisionTask.getCombinedVariablesMap(null, null));
    assertEquals(Collections.emptyMap(),
        shellScriptProvisionTask.getCombinedVariablesMap(Collections.emptyMap(), Collections.emptyMap()));

    Map<String, String> textVariables = new HashMap<>();
    textVariables.put("var1", "val1");
    assertEquals(
        textVariables, shellScriptProvisionTask.getCombinedVariablesMap(textVariables, Collections.emptyMap()));

    Map<String, EncryptedDataDetail> encryptedVariables = new HashMap<>();
    encryptedVariables.put("var2", EncryptedDataDetail.builder().build());

    Mockito.when(encryptionService.getDecryptedValue(any())).thenReturn(new char[] {'a', 'b'});
    Map<String, String> expectedCombinedMap = new HashMap<>();
    expectedCombinedMap.put("var1", "val1");
    expectedCombinedMap.put("var2", "ab");

    assertEquals(
        expectedCombinedMap, shellScriptProvisionTask.getCombinedVariablesMap(textVariables, encryptedVariables));
  }
}
