/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class AwsCodeDeployStateTest extends WingsBaseTest {
  @Mock private AwsStateHelper awsStateHelper;
  @InjectMocks AwsCodeDeployState awsCodeDeployState = new AwsCodeDeployState("awsCodeDeployState");

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testValidateFields() {
    on(awsCodeDeployState).set("bucket", "bucketValue");
    on(awsCodeDeployState).set("key", "keyValue");
    on(awsCodeDeployState).set("bundleType", "bundleTypeValue");
    on(awsCodeDeployState).set("steadyStateTimeout", 0);
    Map<String, String> invalidFields = awsCodeDeployState.validateFields();
    assertThat(invalidFields).isEmpty();

    on(awsCodeDeployState).set("steadyStateTimeout", -10);
    invalidFields = awsCodeDeployState.validateFields();
    assertThat(invalidFields.size()).isEqualTo(1);
    assertThat(invalidFields).containsKey("steadyStateTimeout");

    on(awsCodeDeployState).set("steadyStateTimeout", 20);
    invalidFields = awsCodeDeployState.validateFields();
    assertThat(invalidFields).isEmpty();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetTimeoutMillis() {
    awsCodeDeployState.setSteadyStateTimeout(0);
    assertThat(awsCodeDeployState.getTimeoutMillis()).isNull();

    doReturn(600000).when(awsStateHelper).getTimeout(10);
    awsCodeDeployState.setSteadyStateTimeout(10);
    assertThat(awsCodeDeployState.getTimeoutMillis()).isEqualTo(10 * 60 * 1000);

    doReturn(null).when(awsStateHelper).getTimeout(35792);
    awsCodeDeployState.setSteadyStateTimeout(35792);
    assertThat(awsCodeDeployState.getTimeoutMillis()).isNull();
  }
}
