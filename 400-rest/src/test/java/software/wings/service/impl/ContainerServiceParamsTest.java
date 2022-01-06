/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.SAINATH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;

import groovy.util.logging.Slf4j;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
@OwnedBy(CDP)
public class ContainerServiceParamsTest extends WingsBaseTest {
  @Test
  @Owner(developers = SAINATH)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilities() {
    // awsConfig useEc2IamCredentials = false
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder().build();
    AwsConfig awsConfig = AwsConfig.builder().build();
    containerServiceParams.setSettingAttribute(
        SettingAttribute.Builder.aSettingAttribute().withValue(awsConfig).build());

    List<ExecutionCapability> requiredExecutionCapabilities =
        containerServiceParams.fetchRequiredExecutionCapabilities(null);
    assertThat(requiredExecutionCapabilities.size()).isEqualTo(1);
    assertThat(requiredExecutionCapabilities.get(0) instanceof HttpConnectionExecutionCapability).isTrue();

    // awsConfig useEc2IamCredentials = true, empty tag
    awsConfig.setUseEc2IamCredentials(true);
    awsConfig.setTag("");

    requiredExecutionCapabilities = containerServiceParams.fetchRequiredExecutionCapabilities(null);
    assertThat(requiredExecutionCapabilities.size()).isEqualTo(1);
    assertThat(requiredExecutionCapabilities.get(0) instanceof HttpConnectionExecutionCapability).isTrue();

    // awsConfig useEc2IamCredentials = true, non empty tag
    awsConfig.setTag("test");
    requiredExecutionCapabilities = containerServiceParams.fetchRequiredExecutionCapabilities(null);
    assertThat(requiredExecutionCapabilities.size()).isEqualTo(2);
    assertThat(requiredExecutionCapabilities.get(0) instanceof HttpConnectionExecutionCapability).isTrue();
    assertThat(requiredExecutionCapabilities.get(1) instanceof SelectorCapability).isTrue();
  }
}
