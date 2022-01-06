/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.helpers.ext.pcf.request.CfCommandSetupRequest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class PcfSetupStateExecutionDataTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void shouldGetExecutionSummary() {
    String org = "org";
    String space = "space";
    PcfSetupStateExecutionData pcfSetupStateExecutionData = new PcfSetupStateExecutionData();
    PcfSetupExecutionSummary stepExecutionSummary = pcfSetupStateExecutionData.getStepExecutionSummary();
    assertThat(stepExecutionSummary).isNotNull();
    assertThat(stepExecutionSummary.getMaxInstanceCount()).isEqualTo(0);

    pcfSetupStateExecutionData.setUseCurrentRunningInstanceCount(false);
    stepExecutionSummary = pcfSetupStateExecutionData.getStepExecutionSummary();
    assertThat(stepExecutionSummary).isNotNull();
    assertThat(stepExecutionSummary.getMaxInstanceCount()).isEqualTo(0);

    pcfSetupStateExecutionData.setMaxInstanceCount(2);
    pcfSetupStateExecutionData.setPcfCommandRequest(
        CfCommandSetupRequest.builder().organization(org).space(space).build());
    stepExecutionSummary = pcfSetupStateExecutionData.getStepExecutionSummary();
    assertThat(stepExecutionSummary).isNotNull();
    assertThat(stepExecutionSummary.getMaxInstanceCount()).isEqualTo(2);
    assertThat(stepExecutionSummary.getOrganization()).isEqualTo(org);
    assertThat(stepExecutionSummary.getSpace()).isEqualTo(space);

    pcfSetupStateExecutionData.setUseCurrentRunningInstanceCount(true);
    pcfSetupStateExecutionData.setCurrentRunningInstanceCount(1);
    assertThat(stepExecutionSummary).isNotNull();
    stepExecutionSummary = pcfSetupStateExecutionData.getStepExecutionSummary();
    assertThat(stepExecutionSummary.getMaxInstanceCount()).isEqualTo(1);
    assertThat(stepExecutionSummary.getOrganization()).isEqualTo(org);
    assertThat(stepExecutionSummary.getSpace()).isEqualTo(space);
  }
}
