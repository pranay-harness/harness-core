/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.beans.ExecutionInterruptType.ABORT;
import static io.harness.beans.ExecutionInterruptType.END_EXECUTION;
import static io.harness.beans.ExecutionInterruptType.IGNORE;
import static io.harness.beans.ExecutionInterruptType.MARK_SUCCESS;
import static io.harness.beans.ExecutionInterruptType.ROLLBACK;
import static io.harness.beans.RepairActionCode.MANUAL_INTERVENTION;
import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.beans.yaml.ChangeContext.Builder;
import static software.wings.utils.WingsTestConstants.APP_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionInterruptType;
import io.harness.category.element.UnitTests;
import io.harness.exception.HarnessException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;

import software.wings.beans.FailureStrategy;
import software.wings.beans.FailureStrategy.Yaml;
import software.wings.utils.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FailureStrategyYamlHandlerTest extends CategoryTest {
  private final FailureStrategyYamlHandler failureStrategyYamlHandler = new FailureStrategyYamlHandler();

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnYamlWithTimeoutAndActionAfterTimeout() {
    FailureStrategy failureStrategy = FailureStrategy.builder()
                                          .repairActionCode(MANUAL_INTERVENTION)
                                          .manualInterventionTimeout(60000L)
                                          .actionAfterTimeout(END_EXECUTION)
                                          .build();

    Yaml yaml = failureStrategyYamlHandler.toYaml(failureStrategy, APP_ID);
    assertThat(yaml.getActionAfterTimeout()).isEqualTo(Utils.getStringFromEnum(END_EXECUTION));
    assertThat(yaml.getManualInterventionTimeout()).isEqualTo(60000L);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFailureStrategyWithTimeoutAndActionAfterTimeout() throws HarnessException {
    Yaml yaml = Yaml.builder()
                    .manualInterventionTimeout(60000L)
                    .actionAfterTimeout("end_execution")
                    .repairActionCode("manual_intervention")
                    .build();

    FailureStrategy failureStrategy = failureStrategyYamlHandler.upsertFromYaml(
        Builder.aChangeContext().withYaml(yaml).build(), Collections.emptyList());
    assertThat(failureStrategy.getActionAfterTimeout()).isEqualTo(END_EXECUTION);
    assertThat(failureStrategy.getManualInterventionTimeout()).isEqualTo(60000L);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldThrowInvalidArgumentsExceptionWhenTimeoutIsLessThanOneMinute() {
    Yaml yaml = Yaml.builder()
                    .manualInterventionTimeout(1L)
                    .actionAfterTimeout("end_execution")
                    .repairActionCode("manual_intervention")
                    .build();

    assertThatThrownBy(()
                           -> failureStrategyYamlHandler.upsertFromYaml(
                               Builder.aChangeContext().withYaml(yaml).build(), Collections.emptyList()))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("\"manualInterventionTimeout\" should not be less than 1m (60000)");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldThrowInvalidArgumentsExceptionWhenRetryCountIsLessThanOne() {
    Yaml yaml = Yaml.builder().retryCount(0).repairActionCode("retry").build();

    assertThatThrownBy(()
                           -> failureStrategyYamlHandler.upsertFromYaml(
                               Builder.aChangeContext().withYaml(yaml).build(), Collections.emptyList()))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("\"retryCount\" should be greater than 0");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldThrowInvalidArgumentsExceptionWhenActionAfterTimeoutIsNotProvided() {
    Yaml yaml = Yaml.builder().manualInterventionTimeout(60000L).repairActionCode("manual_intervention").build();

    List<ExecutionInterruptType> allowedActions = Arrays.asList(ABORT, END_EXECUTION, IGNORE, MARK_SUCCESS, ROLLBACK);
    assertThatThrownBy(()
                           -> failureStrategyYamlHandler.upsertFromYaml(
                               Builder.aChangeContext().withYaml(yaml).build(), Collections.emptyList()))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage(String.format(
            "\"actionAfterTimeout\" should not be empty. Please provide valid value: %s", allowedActions));
  }
}
