/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources.yaml;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONFIG_AS_CODE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.security.annotations.AuthRule;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
