/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.arm.taskhandler;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureManagementClient;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.management.ManagementGroupInfo;
import io.harness.azure.model.management.ManagementGroupInfoProperty;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.azure.ManagementGroupData;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.arm.AzureARMTaskParameters;
import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;
import io.harness.delegate.task.azure.arm.response.AzureARMListManagementGroupResponse;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureARMListManagementGroupTaskHandlerTest extends WingsBaseTest {
  @Mock private AzureManagementClient azureManagementClient;
  @Mock private ILogStreamingTaskClient mockLogStreamingTaskClient;

  @Spy @InjectMocks AzureARMListManagementGroupTaskHandler azureARMListManagementGroupTaskHandler;

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecuteTaskInternal() {
    ManagementGroupInfo managementGroupInfo = getManagementGroupInfo();
    doReturn(Collections.singletonList(managementGroupInfo)).when(azureManagementClient).listManagementGroups(any());

    AzureARMTaskResponse azureARMTaskResponse = azureARMListManagementGroupTaskHandler.executeTaskInternal(
        new AzureARMTaskParameters(), AzureConfig.builder().build(), mockLogStreamingTaskClient);

    assertThat(azureARMTaskResponse).isNotNull();
    assertThat(azureARMTaskResponse).isInstanceOf(AzureARMListManagementGroupResponse.class);
    AzureARMListManagementGroupResponse listManagementGroupResponse =
        (AzureARMListManagementGroupResponse) azureARMTaskResponse;
    assertThat(listManagementGroupResponse.getMngGroups().size()).isEqualTo(1);
    ManagementGroupData managementGroupData = listManagementGroupResponse.getMngGroups().get(0);
    assertThat(managementGroupData.getId()).isEqualTo("MG_ID");
    assertThat(managementGroupData.getName()).isEqualTo("MG_NAME");
    assertThat(managementGroupData.getDisplayName()).isEqualTo("MG_DISPLAY_NAME");
  }

  @NotNull
  private ManagementGroupInfo getManagementGroupInfo() {
    ManagementGroupInfoProperty properties = new ManagementGroupInfoProperty();
    properties.setDisplayName("MG_DISPLAY_NAME");
    properties.setTenantId("TENANT_ID");
    ManagementGroupInfo managementGroupInfo = new ManagementGroupInfo();
    managementGroupInfo.setName("MG_NAME");
    managementGroupInfo.setId("MG_ID");
    managementGroupInfo.setProperties(properties);
    return managementGroupInfo;
  }
}
