/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.cloudformation.request;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.beans.AwsConfig;
import software.wings.beans.GitConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CloudFormationCreateStackRequestTest extends CategoryTest {
  @Test
  @Owner(developers = OwnerRule.PARDHA)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilitiesWithSingleDelegateSelector() {
    CloudFormationCreateStackRequest cloudFormationCreateStackRequest =
        CloudFormationCreateStackRequest.builder()
            .awsConfig(AwsConfig.builder().build())
            .gitConfig(GitConfig.builder().delegateSelectors(Collections.singletonList("primary")).build())
            .build();
    assertThat(cloudFormationCreateStackRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.HTTP, CapabilityType.SELECTORS);
  }

  @Test
  @Owner(developers = OwnerRule.PARDHA)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilitiesWithMultipleDelegateSelectors() {
    CloudFormationCreateStackRequest cloudFormationCreateStackRequest =
        CloudFormationCreateStackRequest.builder()
            .awsConfig(AwsConfig.builder().build())
            .gitConfig(GitConfig.builder().delegateSelectors(Arrays.asList("primary", "delegate")).build())
            .build();
    assertThat(cloudFormationCreateStackRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.HTTP, CapabilityType.SELECTORS);
  }

  @Test
  @Owner(developers = OwnerRule.PARDHA)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilitiesWithoutDelegateSelectors() {
    CloudFormationCreateStackRequest cloudFormationCreateStackRequest = CloudFormationCreateStackRequest.builder()
                                                                            .awsConfig(AwsConfig.builder().build())
                                                                            .gitConfig(GitConfig.builder().build())
                                                                            .build();
    assertThat(cloudFormationCreateStackRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.HTTP);
  }
}
