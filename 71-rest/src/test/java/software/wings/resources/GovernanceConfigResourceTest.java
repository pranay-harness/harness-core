package software.wings.resources;

import static io.harness.rule.OwnerRule.HINGER;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DEPLOYMENT_FREEZES;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.resources.governance.GovernanceConfigResource;
import software.wings.security.annotations.AuthRule;

import java.lang.reflect.Method;

public class GovernanceConfigResourceTest extends CategoryTest {
  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrect() throws NoSuchMethodException {
    Method method = GovernanceConfigResource.class.getDeclaredMethod("update", String.class, GovernanceConfig.class);
    AuthRule annotation = method.getAnnotation(AuthRule.class);
    assertThat(annotation.permissionType()).isEqualTo(MANAGE_DEPLOYMENT_FREEZES);
  }
}
