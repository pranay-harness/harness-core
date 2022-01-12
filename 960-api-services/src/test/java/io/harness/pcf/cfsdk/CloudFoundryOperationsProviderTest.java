/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pcf.cfsdk;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.rule.Owner;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryOperationsProviderTest extends CategoryTest {
  @Mock private ConnectionContextProvider connectionContextProvider;
  @Mock private CloudFoundryClientProvider cloudFoundryClientProvider;

  @InjectMocks private CloudFoundryOperationsProvider cloudFoundryOperationsProvider;

  @Test
  @Owner(developers = {ANSHUL, IVAN})
  @Category(UnitTests.class)
  public void testGetCloudFoundryOperationsWrapper() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig = getCfRequestConfig();
    cfRequestConfig.setUserName("username");
    cfRequestConfig.setPassword("password");
    cfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    ConnectionContext connectionContextMock = mock(ConnectionContext.class);
    CloudFoundryClient cloudFoundryClientMock = mock(CloudFoundryClient.class);

    doReturn(connectionContextMock).when(connectionContextProvider).getConnectionContext(cfRequestConfig);
    doReturn(cloudFoundryClientMock)
        .when(cloudFoundryClientProvider)
        .getCloudFoundryClient(eq(cfRequestConfig), any(ConnectionContext.class));

    CloudFoundryOperationsWrapper cloudFoundryOperationsWrapper =
        cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(cfRequestConfig);

    assertThat(cloudFoundryOperationsWrapper).isNotNull();
    assertThat(cloudFoundryOperationsWrapper.getCloudFoundryOperations()).isNotNull();
    assertThat(cloudFoundryOperationsWrapper.getCloudFoundryOperations())
        .isInstanceOf(DefaultCloudFoundryOperations.class);
  }

  @Test
  @Owner(developers = {ANSHUL, IVAN})
  @Category(UnitTests.class)
  public void testGetCFOperationsWrapperForConnectionContextException() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig = getCfRequestConfig();
    doThrow(new PivotalClientApiException("Unable to get connection context"))
        .when(connectionContextProvider)
        .getConnectionContext(cfRequestConfig);

    try {
      cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(cfRequestConfig);
      fail("Should not reach here.");
    } catch (PivotalClientApiException e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception while creating CloudFoundryOperations: Unable to get connection context");
    }
  }

  @Test
  @Owner(developers = {ANSHUL, IVAN})
  @Category(UnitTests.class)
  public void testGetCFOperationsWrapperForClientProviderException() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig = getCfRequestConfig();
    doThrow(new PivotalClientApiException("Unable to get client provider"))
        .when(cloudFoundryClientProvider)
        .getCloudFoundryClient(eq(cfRequestConfig), any(ConnectionContext.class));

    try {
      cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(cfRequestConfig);
      fail("Should not reach here.");
    } catch (PivotalClientApiException e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception while creating CloudFoundryOperations: Unable to get client provider");
    }
  }

  private CfRequestConfig getCfRequestConfig() {
    return CfRequestConfig.builder().timeOutIntervalInMins(1).orgName("org").applicationName("app").build();
  }
}
