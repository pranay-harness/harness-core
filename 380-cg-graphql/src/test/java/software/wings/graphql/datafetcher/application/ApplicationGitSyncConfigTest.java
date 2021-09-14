/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.application;

import static io.harness.rule.OwnerRule.HINGER;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONFIG_AS_CODE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationGitSyncConfigInput;
import software.wings.security.annotations.AuthRule;

import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApplicationGitSyncConfigTest extends CategoryTest {
  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrectForApplicationUpdateGitSyncConfig() throws NoSuchMethodException {
    Method method = UpdateApplicationGitSyncConfigDataFetcher.class.getDeclaredMethod(
        "mutateAndFetch", QLUpdateApplicationGitSyncConfigInput.class, MutationContext.class);
    AuthRule annotation = method.getAnnotation(AuthRule.class);
    assertThat(annotation.permissionType()).isEqualTo(MANAGE_CONFIG_AS_CODE);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrectForApplicationUpdateGitSyncConfigStatus() throws NoSuchMethodException {
    Method method = UpdateApplicationGitSyncConfigStatusDataFetcher.class.getDeclaredMethod(
        "mutateAndFetch", QLUpdateApplicationGitSyncConfigInput.class, MutationContext.class);
    AuthRule annotation = method.getAnnotation(AuthRule.class);
    assertThat(annotation.permissionType()).isEqualTo(MANAGE_CONFIG_AS_CODE);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrectForApplicationRemoveGitSync() throws NoSuchMethodException {
    Method method = RemoveApplicationGitSyncConfigDataFetcher.class.getDeclaredMethod(
        "mutateAndFetch", QLUpdateApplicationGitSyncConfigInput.class, MutationContext.class);
    AuthRule annotation = method.getAnnotation(AuthRule.class);
    assertThat(annotation.permissionType()).isEqualTo(MANAGE_CONFIG_AS_CODE);
  }
}
