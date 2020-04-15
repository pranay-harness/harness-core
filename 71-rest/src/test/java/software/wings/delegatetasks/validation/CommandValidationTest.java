package software.wings.delegatetasks.validation;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.delegatetasks.validation.CommandValidation.ALWAYS_TRUE_CRITERIA;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.command.CommandExecutionContext;

import java.util.List;

public class CommandValidationTest extends WingsBaseTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidate_ECS() throws Exception {
    CommandExecutionContext commandExecutionContext = new CommandExecutionContext();
    commandExecutionContext.setDeploymentType("ECS");

    CommandValidation commandValidation = new CommandValidation("1",
        DelegateTask.builder()
            .data(TaskData.builder().parameters(new Object[] {"", commandExecutionContext}).build())
            .build(),
        null);

    List<DelegateConnectionResult> validateList = commandValidation.validate();
    assertThat(validateList).isNotEmpty();
    assertThat(validateList.size()).isEqualTo(1);

    DelegateConnectionResult validateResult = validateList.get(0);
    assertThat(validateResult.isValidated()).isTrue();
    assertThat(validateResult.getCriteria()).isEqualTo(ALWAYS_TRUE_CRITERIA);
  }
}
