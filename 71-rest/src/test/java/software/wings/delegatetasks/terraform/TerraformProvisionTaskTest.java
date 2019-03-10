package software.wings.delegatetasks.terraform;

import static org.junit.Assert.assertEquals;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;

import io.harness.delegate.beans.TaskData;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTask;
import software.wings.delegatetasks.TerraformProvisionTask;
import software.wings.utils.WingsTestConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TerraformProvisionTaskTest extends WingsBaseTest {
  TerraformProvisionTask terraformProvisionTask = new TerraformProvisionTask(WingsTestConstants.DELEGATE_ID,
      DelegateTask.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).data(TaskData.builder().build()).build(),
      delegateTaskResponse -> {}, () -> true);

  @Test
  public void getTargetsArgsTest() {
    assertEquals("", terraformProvisionTask.getTargetArgs(null));
    assertEquals("", terraformProvisionTask.getTargetArgs(Collections.EMPTY_LIST));

    List<String> targets = new ArrayList<>(Arrays.asList("target1", "target2"));

    assertEquals("-target=target1 -target=target2 ", terraformProvisionTask.getTargetArgs(targets));
  }
}
