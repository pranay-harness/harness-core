package software.wings.delegatetasks.terraform;

import static io.harness.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static org.junit.Assert.assertEquals;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.TerraformProvisionTask;
import software.wings.utils.WingsTestConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TerraformProvisionTaskTest extends WingsBaseTest {
  TerraformProvisionTask terraformProvisionTask = new TerraformProvisionTask(WingsTestConstants.DELEGATE_ID,
      DelegateTask.builder().async(true).data(TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
      delegateTaskResponse -> {}, () -> true);

  @Test
  @Category(UnitTests.class)
  public void getTargetsArgsTest() {
    assertEquals("", terraformProvisionTask.getTargetArgs(null));
    assertEquals("", terraformProvisionTask.getTargetArgs(Collections.EMPTY_LIST));

    List<String> targets = new ArrayList<>(Arrays.asList("target1", "target2"));

    assertEquals("-target=target1 -target=target2 ", terraformProvisionTask.getTargetArgs(targets));
  }
}
