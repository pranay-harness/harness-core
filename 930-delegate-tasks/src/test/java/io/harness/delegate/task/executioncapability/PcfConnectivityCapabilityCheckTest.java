/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.rule.OwnerRule.ANIL;
import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.PcfConnectivityCapability;
import io.harness.rule.Owner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class PcfConnectivityCapabilityCheckTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private static final String ENDPOINT_URL = "endpointUrl.ca";
  private final PcfConnectivityCapability pcfConnectivityCapability =
      PcfConnectivityCapability.builder().endpointUrl(ENDPOINT_URL).build();

  @Spy private PcfConnectivityCapabilityCheck pcfConnectivityCapabilityCheck;

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldPerformCapabilityCheck() {
    doReturn(true).when(pcfConnectivityCapabilityCheck).isEndpointConnectable(eq(ENDPOINT_URL), eq("https://"));
    doReturn(false).when(pcfConnectivityCapabilityCheck).isEndpointConnectable(eq(ENDPOINT_URL), eq("http://"));
    CapabilityResponse capabilityResponse =
        pcfConnectivityCapabilityCheck.performCapabilityCheck(pcfConnectivityCapability);
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldNotPassCapabilityCheck() {
    doReturn(false).when(pcfConnectivityCapabilityCheck).isEndpointConnectable(eq(ENDPOINT_URL), eq("https://"));
    doReturn(false).when(pcfConnectivityCapabilityCheck).isEndpointConnectable(eq(ENDPOINT_URL), eq("http://"));
    CapabilityResponse capabilityResponse =
        pcfConnectivityCapabilityCheck.performCapabilityCheck(pcfConnectivityCapability);
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isFalse();
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void validatePcfEndPointURL() {
    String pcfUrl = "api.pivotal.io";
    String expectedCapabilityUrl = "Pcf:" + pcfUrl;
    PcfConnectivityCapability capabilityCheck = PcfConnectivityCapability.builder().endpointUrl(pcfUrl).build();
    String actualCapabilityUrl = capabilityCheck.fetchCapabilityBasis();

    // CDP-14589
    assertThat(actualCapabilityUrl).isEqualTo(expectedCapabilityUrl);
  }
}
