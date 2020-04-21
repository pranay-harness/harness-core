package software.wings.delegatetasks.validation.terraform;

import static io.harness.exception.WingsException.USER;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.io.IOException;

public class TerraformTaskUtilsTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetGitExceptionMessageIfExists() {
    Exception ex;

    ex = new JGitInternalException("jgit-exception", new IOException("out of memory"));
    assertThat(TerraformTaskUtils.getGitExceptionMessageIfExists(ex)).isEqualTo("out of memory");

    ex = new JGitInternalException("jgit-exception");
    assertThat(TerraformTaskUtils.getGitExceptionMessageIfExists(ex)).isEqualTo("jgit-exception");

    ex = new InvalidRequestException("msg", USER);
    assertThat(TerraformTaskUtils.getGitExceptionMessageIfExists(ex)).isEqualTo("Invalid request: msg");
  }
}