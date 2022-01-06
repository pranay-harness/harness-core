/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.execution.utils;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class PlanCreationContextUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetGlobalContextFields() {
    Map<String, PlanCreationContextValue> globalContext =
        ImmutableMap.<String, PlanCreationContextValue>builder()
            .put("accountId", PlanCreationContextValue.newBuilder().setStringValue("aid").build())
            .put("orgIdentifier", PlanCreationContextValue.newBuilder().setStringValue("oid").build())
            .put("projectIdentifier", PlanCreationContextValue.newBuilder().setStringValue("pid").build())
            .put("pipelineIdentifier", PlanCreationContextValue.newBuilder().setStringValue("pipid").build())
            .put("triggerInfo",
                PlanCreationContextValue.newBuilder()
                    .setMetadata(ExecutionMetadata.newBuilder()
                                     .setTriggerInfo(
                                         ExecutionTriggerInfo.newBuilder().setTriggerType(TriggerType.MANUAL).build())
                                     .build())
                    .build())
            .put("eventPayload", PlanCreationContextValue.newBuilder().setStringValue("payload").build())
            .build();

    assertThat(PlanCreationContextUtils.getAccountId(globalContext)).isEqualTo("aid");
    assertThat(PlanCreationContextUtils.getOrgIdentifier(globalContext)).isEqualTo("oid");
    assertThat(PlanCreationContextUtils.getProjectIdentifier(globalContext)).isEqualTo("pid");
    assertThat(PlanCreationContextUtils.getPipelineIdentifier(globalContext)).isEqualTo("pipid");
    assertThat(PlanCreationContextUtils.getTriggerInfo(globalContext).getTriggerType()).isEqualTo(TriggerType.MANUAL);
    assertThat(PlanCreationContextUtils.getEventPayload(globalContext)).isEqualTo("payload");
  }
}
