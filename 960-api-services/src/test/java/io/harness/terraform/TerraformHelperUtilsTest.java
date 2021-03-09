package io.harness.terraform;

import static io.harness.rule.OwnerRule.ROHITKARELIA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TerraformHelperUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testgenerateCommandFlagsString() {
    List<String> listofArgs = Arrays.asList("arg1", "arg2");
    String result = TerraformHelperUtils.generateCommandFlagsString(listofArgs, "-command");
    assertThat(result).isNotNull();
    result.equals("-command=arg1 -command=arg2");
  }
}