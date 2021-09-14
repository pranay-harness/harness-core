/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.rbac;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineReferredEntityPermissionHelperTest extends CategoryTest {
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetPermissionForGivenTypeWithoutNew() {
    EntityType entityType = EntityType.CONNECTORS;
    String permission = PipelineReferredEntityPermissionHelper.getPermissionForGivenType(entityType, false);
    assertThat(permission).isEqualTo("core_connector_access");

    entityType = EntityType.SECRETS;
    permission = PipelineReferredEntityPermissionHelper.getPermissionForGivenType(entityType, false);
    assertThat(permission).isEqualTo("core_secret_access");

    entityType = EntityType.SERVICE;
    permission = PipelineReferredEntityPermissionHelper.getPermissionForGivenType(entityType, false);
    assertThat(permission).isEqualTo("core_service_access");

    entityType = EntityType.ENVIRONMENT;
    permission = PipelineReferredEntityPermissionHelper.getPermissionForGivenType(entityType, false);
    assertThat(permission).isEqualTo("core_environment_access");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetPermissionForGivenTypeWithNew() {
    EntityType entityType = EntityType.CONNECTORS;
    String permission = PipelineReferredEntityPermissionHelper.getPermissionForGivenType(entityType, true);
    assertThat(permission).isEqualTo("core_connector_edit");

    entityType = EntityType.SECRETS;
    permission = PipelineReferredEntityPermissionHelper.getPermissionForGivenType(entityType, true);
    assertThat(permission).isEqualTo("core_secret_edit");

    entityType = EntityType.SERVICE;
    permission = PipelineReferredEntityPermissionHelper.getPermissionForGivenType(entityType, true);
    assertThat(permission).isEqualTo("core_service_edit");

    entityType = EntityType.ENVIRONMENT;
    permission = PipelineReferredEntityPermissionHelper.getPermissionForGivenType(entityType, true);
    assertThat(permission).isEqualTo("core_environment_edit");
  }
}
