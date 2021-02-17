package software.wings.beans;

import static io.harness.rule.OwnerRule.UNKNOWN;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.command.CommandUnitType;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Created by peeyushaggarwal on 6/6/16.
 */
@RunWith(JUnitParamsRunner.class)
@Ignore("TODO: This test is failing in bazel. Changes are required from the owner to make it work in bazel")
public class CommandUnitTypeTest extends CategoryTest {
  private Object[][] getData() {
    Object[][] data = new Object[CommandUnitType.values().length][1];

    for (int i = 0; i < CommandUnitType.values().length; i++) {
      data[i][0] = UPPER_UNDERSCORE.to(UPPER_CAMEL, CommandUnitType.values()[i].name());
    }
    return data;
  }

  /**
   * Should create new instance for.
   *
   * @param commandUnitTypeName the command unit type name
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  @Parameters(method = "getData")
  public void shouldCreateNewInstanceFor(String commandUnitTypeName) throws Exception {
    CommandUnitType commandUnitType = CommandUnitType.valueOf(UPPER_CAMEL.to(UPPER_UNDERSCORE, commandUnitTypeName));
    assertThat(commandUnitType).isNotNull();
    assertThat(commandUnitType.newInstance("")).isNotNull();
  }
}
