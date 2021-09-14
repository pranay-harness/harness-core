/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.command;

import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability;
import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.VGLIJIN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.rule.Owner;

import software.wings.beans.GcpConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

public class GcbTaskParamsTest extends CategoryTest {
  private final List<ExecutionCapability> capabilities = new ArrayList<>();
  private static final GcpConfig gcpConfig = Mockito.mock(GcpConfig.class);
  private static final GcbTaskParams gcbTaskParam = GcbTaskParams.builder().gcpConfig(gcpConfig).build();
  private static final String DELEGATE_SELECTOR = "delegateSelector";

  @Before
  public void setUp() {
    capabilities.add(buildHttpConnectionExecutionCapability("GCS_URL", null));
    when(gcpConfig.isUseDelegateSelectors()).thenReturn(false);
    when(gcpConfig.fetchRequiredExecutionCapabilities(null)).thenReturn(capabilities);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilitiesTest() {
    assertThat(gcbTaskParam.fetchRequiredExecutionCapabilities(null)).isEqualTo(capabilities);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnListOfExecutionCapabilitiesWithSelectorCapability() {
    when(gcpConfig.isUseDelegateSelectors()).thenReturn(true);
    when(gcpConfig.getDelegateSelectors()).thenReturn(Collections.singletonList(DELEGATE_SELECTOR));

    assertThat(gcbTaskParam.fetchRequiredExecutionCapabilities(null)).hasSize(2);
    SelectorCapability selector = (SelectorCapability) gcbTaskParam.fetchRequiredExecutionCapabilities(null)
                                      .stream()
                                      .filter(executionCapability -> executionCapability instanceof SelectorCapability)
                                      .findFirst()
                                      .orElseThrow(NullPointerException::new);
    assertThat(selector.getSelectors()).contains(DELEGATE_SELECTOR);
  }
}
