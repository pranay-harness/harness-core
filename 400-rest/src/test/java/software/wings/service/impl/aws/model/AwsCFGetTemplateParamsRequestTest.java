/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.aws.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.beans.GitConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AwsCFGetTemplateParamsRequestTest extends CategoryTest {
  @Test
  @Owner(developers = OwnerRule.PARDHA)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilitiesWithSingleDelegateSelector() {
    AwsCFGetTemplateParamsRequest awsCFGetTemplateParamsRequest =
        AwsCFGetTemplateParamsRequest.builder()
            .gitConfig(GitConfig.builder().delegateSelectors(Collections.singletonList("primary")).build())
            .build();
    assertThat(awsCFGetTemplateParamsRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.SELECTORS);
  }

  @Test
  @Owner(developers = OwnerRule.PARDHA)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilitiesWithMultipleDelegateSelectors() {
    AwsCFGetTemplateParamsRequest awsCFGetTemplateParamsRequest =
        AwsCFGetTemplateParamsRequest.builder()
            .gitConfig(GitConfig.builder().delegateSelectors(Arrays.asList("primary", "delegate")).build())
            .build();
    assertThat(awsCFGetTemplateParamsRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.SELECTORS);
  }

  @Test
  @Owner(developers = OwnerRule.PARDHA)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilitiesWithoutDelegateSelectors() {
    AwsCFGetTemplateParamsRequest awsCFGetTemplateParamsRequest =
        AwsCFGetTemplateParamsRequest.builder().gitConfig(GitConfig.builder().build()).build();
    assertThat(awsCFGetTemplateParamsRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .isEmpty();
  }
}
