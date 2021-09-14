/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc.auth;

import static io.harness.rule.OwnerRule.VIKAS;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import io.harness.security.ServiceTokenGenerator;

import io.grpc.CallCredentials;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

public class ServiceAuthCallCredentialsTest extends CategoryTest {
  private ServiceAuthCallCredentials serviceAuthCallCredentials;
  private ServiceTokenGenerator serviceTokenGenerator;
  private CallCredentials.RequestInfo requestInfo;
  private CallCredentials.MetadataApplier metadataApplier;
  private final String SERVICE_SECRET = "test_secret";
  private final String SERVICE_ID = "manager";
  private final String ERROR_MSG = "secretKey cannot be empty";
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    serviceTokenGenerator = mock(ServiceTokenGenerator.class);
    requestInfo = mock(CallCredentials.RequestInfo.class);
    metadataApplier = mock(CallCredentials.MetadataApplier.class);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testApplyRequestMetadata() {
    when(serviceTokenGenerator.getServiceToken(anyString())).thenReturn(SERVICE_SECRET);
    serviceAuthCallCredentials = new ServiceAuthCallCredentials(SERVICE_SECRET, serviceTokenGenerator, SERVICE_ID);
    serviceAuthCallCredentials.applyRequestMetadata(requestInfo, directExecutor(), metadataApplier);
    verify(metadataApplier).apply(any());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testApplyRequestMetadata_WhenSecretNotPresent() {
    when(serviceTokenGenerator.getServiceToken(anyString())).thenThrow(new InvalidArgumentsException(ERROR_MSG, null));
    serviceAuthCallCredentials = new ServiceAuthCallCredentials(SERVICE_SECRET, serviceTokenGenerator, SERVICE_ID);
    thrown.expect(InvalidArgumentsException.class);
    thrown.expectMessage(ERROR_MSG);
    serviceAuthCallCredentials.applyRequestMetadata(requestInfo, directExecutor(), metadataApplier);
  }
}
