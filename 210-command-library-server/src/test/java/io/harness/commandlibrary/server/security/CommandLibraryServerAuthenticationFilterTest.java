/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.commandlibrary.server.security;

import static io.harness.commandlibrary.common.CommandLibraryConstants.MANAGER_CLIENT_ID;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.commandlibrary.server.app.CommandLibraryServerConfig;
import io.harness.commandlibrary.server.beans.ServiceSecretConfig;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.resources.commandlibrary.CommandLibraryResource;

import java.io.IOException;
import java.net.URI;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.Test.None;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class CommandLibraryServerAuthenticationFilterTest extends CategoryTest {
  @Mock CommandLibraryServerConfig commandLibraryServerConfig;
  @Mock ResourceInfo resourceInfo;

  @InjectMocks
  @Spy
  CommandLibraryServerAuthenticationFilter authFilter = new CommandLibraryServerAuthenticationFilter();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    doReturn(ServiceSecretConfig.builder().managerToCommandLibraryServiceSecret("secret").build())
        .when(commandLibraryServerConfig)
        .getServiceSecretConfig();
    doReturn(ResourceInfo.class).when(resourceInfo).getResourceClass();
    doReturn(CommandLibraryResource.class.getDeclaredMethods()[0]).when(resourceInfo).getResourceMethod();
  }

  @Test(expected = None.class)
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category({UnitTests.class})
  public void test_filter() throws IOException {
    final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    doReturn(HttpMethod.GET).when(requestContext).getMethod();
    doReturn(MANAGER_CLIENT_ID + SPACE + "token").when(requestContext).getHeaderString(HttpHeaders.AUTHORIZATION);

    final UriInfo mockUriInfo = mock(UriInfo.class);
    doReturn(URI.create("/command-library-service/command-stores")).when(mockUriInfo).getAbsolutePath();
    doReturn(mockUriInfo).when(requestContext).getUriInfo();

    doNothing().when(authFilter).validateTokenUsingSecret("token", "secret");
    authFilter.filter(requestContext);
  }
}
