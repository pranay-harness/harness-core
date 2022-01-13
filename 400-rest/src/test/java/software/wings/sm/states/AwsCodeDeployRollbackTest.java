/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ANSHUL;

import static software.wings.api.AwsCodeDeployRequestElement.AWS_CODE_DEPLOY_REQUEST_PARAM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class AwsCodeDeployRollbackTest extends WingsBaseTest {
  @Mock private ExecutionContextImpl context;

  @InjectMocks AwsCodeDeployRollback awsCodeDeployRollback = new AwsCodeDeployRollback("awsCodeDeployRollback");

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecute() {
    when(context.getContextElement(ContextElementType.PARAM, AWS_CODE_DEPLOY_REQUEST_PARAM)).thenReturn(null);
    ExecutionResponse response = awsCodeDeployRollback.execute(context);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SKIPPED);
    assertThat(response.getStateExecutionData().getErrorMsg()).isEqualTo("No context found for rollback. Skipping.");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testValidateFields() {
    on(awsCodeDeployRollback).set("steadyStateTimeout", 0);
    on(awsCodeDeployRollback).set("rollback", true);
    Map<String, String> invalidFields = awsCodeDeployRollback.validateFields();
    assertThat(invalidFields).isEmpty();

    on(awsCodeDeployRollback).set("steadyStateTimeout", -10);
    invalidFields = awsCodeDeployRollback.validateFields();
    assertThat(invalidFields.size()).isEqualTo(1);
    assertThat(invalidFields).containsKey("steadyStateTimeout");

    on(awsCodeDeployRollback).set("steadyStateTimeout", 20);
    invalidFields = awsCodeDeployRollback.validateFields();
    assertThat(invalidFields).isEmpty();
  }
}
