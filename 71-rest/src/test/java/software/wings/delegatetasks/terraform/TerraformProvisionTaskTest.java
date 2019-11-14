package software.wings.delegatetasks.terraform;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.OwnerRule.Owner;
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
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void getTargetsArgsTest() {
    assertThat(terraformProvisionTask.getTargetArgs(null)).isEqualTo("");
    assertThat(terraformProvisionTask.getTargetArgs(Collections.EMPTY_LIST)).isEqualTo("");

    List<String> targets = new ArrayList<>(Arrays.asList("target1", "target2"));

    assertThat(terraformProvisionTask.getTargetArgs(targets)).isEqualTo("-target=target1 -target=target2 ");
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testParseOutput() {
    String workspaceCommandOutput = "* w1\n  w2\n w3";
    assertThat(Arrays.asList("w1", "w2", "w3").equals(terraformProvisionTask.parseOutput(workspaceCommandOutput)))
        .isTrue();
  }
}
