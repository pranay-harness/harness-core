package software.wings.resources.yaml;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONFIG_AS_CODE;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.security.annotations.AuthRule;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class YamlResourceTest extends CategoryTest {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrect() {
    final Method[] methods = YamlResource.class.getMethods();
    Set<String> methodWithManageConfigPermission =
        new HashSet<>(Arrays.asList("fullSyncAccount", "processYamlFilesAsZip", "discardGitSyncError",
            "discardGitSyncError", "updateGitConfig", "delete", "saveGitConfig", "pushDirectory"));
    for (Method method : methods) {
      if (methodWithManageConfigPermission.contains(method.getName())) {
        AuthRule annotation = method.getAnnotation(AuthRule.class);
        assertThat(annotation.permissionType()).isEqualTo(MANAGE_CONFIG_AS_CODE);
      } else {
        AuthRule annotation = method.getAnnotation(AuthRule.class);
        if (annotation == null) {
          continue;
        }
        assertThat(annotation.permissionType()).isNotEqualTo(MANAGE_CONFIG_AS_CODE);
      }
    }
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void checkPermissionsForYAMLCrudApis() {
    final Method[] methods = YamlResource.class.getMethods();
    Set<String> methodsWithAccountManagementPermission =
        new HashSet<>(Arrays.asList("deleteYAMLEntities", "upsertYAMLEntities", "upsertYAMLEntity"));
    for (Method method : methods) {
      if (methodsWithAccountManagementPermission.contains(method.getName())) {
        AuthRule annotation = method.getAnnotation(AuthRule.class);
        assertThat(annotation.permissionType()).isEqualTo(ACCOUNT_MANAGEMENT);
      }
    }
  }
}