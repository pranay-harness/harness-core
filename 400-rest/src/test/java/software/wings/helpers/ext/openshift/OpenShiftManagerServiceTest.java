/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.helpers.ext.openshift;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.utils.ApplicationManifestUtils;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class OpenShiftManagerServiceTest extends WingsBaseTest {
  @Mock ApplicationManifestUtils applicationManifestUtils;
  @InjectMocks @Inject private OpenShiftManagerService openShiftManagerService;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testIsOpenShiftManifestConfig() {
    // openshift manifest
    ExecutionContext executionContext = new ExecutionContextImpl(null);
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(StoreType.OC_TEMPLATES).build();

    doReturn(applicationManifest).when(applicationManifestUtils).getApplicationManifestForService(executionContext);

    assertThat(openShiftManagerService.isOpenShiftManifestConfig(executionContext)).isTrue();

    // non-openshift manifest
    applicationManifest = ApplicationManifest.builder().storeType(StoreType.Local).build();

    doReturn(applicationManifest).when(applicationManifestUtils).getApplicationManifestForService(executionContext);

    assertThat(openShiftManagerService.isOpenShiftManifestConfig(executionContext)).isFalse();

    // should throw exception if manifest not found
    doReturn(null).when(applicationManifestUtils).getApplicationManifestForService(executionContext);
    assertThatThrownBy(() -> openShiftManagerService.isOpenShiftManifestConfig(executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Manifest Config at Service can't be  empty");
  }
}
