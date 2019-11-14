package software.wings.service.impl;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.utils.GitUtilsDelegate;

public class GitUtilsDelegateTest extends WingsBaseTest {
  @Inject private GitUtilsDelegate gitUtilsDelegate;

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testGetRequestDataFromFile() {
    String nonExistentPath = "/thisPathDoesNotExistOnDelegate/nonExistentFile.yaml";
    assertThatThrownBy(() -> gitUtilsDelegate.getRequestDataFromFile(nonExistentPath))
        .isInstanceOf(RuntimeException.class);
  }
}
