/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.graphql.schema.type.permissions.QLAccountPermissionType;
import software.wings.graphql.schema.type.permissions.QLActions;

import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * This test is to make sure QL Permissions/Actions
 * are in sync with actual Permissions/Actions.
 *
 * created by ujjawal
 */
@OwnedBy(PL)
@TargetModule(HarnessModule._970_RBAC_CORE)
public class PermissionAttributeTest extends WingsBaseTest {
  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAddedPermissionsAreInSync() {
    assertThat(PermissionAttribute.PermissionType.values().length).isEqualTo(45);
    assertThat(QLAccountPermissionType.values().length).isEqualTo(30);
    assertThat(PermissionAttribute.PermissionType.values().length - 42)
        .isEqualTo(QLAccountPermissionType.values().length - 27);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAddedActionsAreInSync() {
    assertThat(PermissionAttribute.Action.values().length).isEqualTo(10);
    assertThat(QLActions.values().length).isEqualTo(8);
    assertThat(PermissionAttribute.Action.values().length - 10).isEqualTo(QLActions.values().length - 8);
  }
}
