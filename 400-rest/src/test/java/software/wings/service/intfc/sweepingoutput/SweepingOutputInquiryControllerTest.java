/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.sweepingoutput;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.sm.ContextElement;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SweepingOutputInquiryControllerTest extends WingsBaseTest {
  private final String infraDefinitionId = generateUuid();
  private final String executionUuid = generateUuid();
  private final String appId = generateUuid();
  private final String stateExecutionInstanceId = generateUuid();
  private final String phaseElementId = generateUuid();
  private StateExecutionInstance stateExecutionInstance;

  @Before
  public void setup() {
    LinkedList<ContextElement> contextElements = new LinkedList<>();
    ContextElement phaseElement = PhaseElement.builder()
                                      .uuid(phaseElementId)
                                      .infraDefinitionId(infraDefinitionId)
                                      .rollback(false)
                                      .phaseName("Phase 1")
                                      .phaseNameForRollback("Rollback Phase 1")
                                      .onDemandRollback(false)
                                      .build();
    contextElements.add(phaseElement);

    stateExecutionInstance = aStateExecutionInstance()
                                 .uuid(stateExecutionInstanceId)
                                 .appId(appId)
                                 .executionUuid(executionUuid)
                                 .stateType(StateType.AWS_NODE_SELECT.name())
                                 .displayName(StateType.AWS_NODE_SELECT.name())
                                 .stateName(StateType.AWS_NODE_SELECT.name())
                                 .contextElements(contextElements)
                                 .build();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void obtainFromStateExecutionInstance() {
    SweepingOutputInquiry sweepingOutputInquiry =
        SweepingOutputInquiryController.obtainFromStateExecutionInstance(stateExecutionInstance, "Prefix");
    assertThat(sweepingOutputInquiry).isNotNull();
    assertThat(sweepingOutputInquiry.getAppId()).isEqualTo(appId);
    assertThat(sweepingOutputInquiry.getWorkflowExecutionId()).isEqualTo(executionUuid);
    assertThat(sweepingOutputInquiry.getStateExecutionId()).isEqualTo(stateExecutionInstanceId);
    assertThat(sweepingOutputInquiry.getPhaseExecutionId()).isEqualTo(executionUuid + phaseElementId + "Phase 1");
    assertThat(sweepingOutputInquiry.getName()).isEqualTo("Prefix" + stateExecutionInstance.getDisplayName().trim());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void obtainFromStateExecutionInstanceWithoutName() {
    SweepingOutputInquiry sweepingOutputInquiry =
        SweepingOutputInquiryController.obtainFromStateExecutionInstanceWithoutName(stateExecutionInstance);
    assertThat(sweepingOutputInquiry).isNotNull();
    assertThat(sweepingOutputInquiry.getAppId()).isEqualTo(appId);
    assertThat(sweepingOutputInquiry.getWorkflowExecutionId()).isEqualTo(executionUuid);
    assertThat(sweepingOutputInquiry.getStateExecutionId()).isEqualTo(stateExecutionInstanceId);
    assertThat(sweepingOutputInquiry.getPhaseExecutionId()).isEqualTo(executionUuid + phaseElementId + "Phase 1");
    assertThat(sweepingOutputInquiry.getName()).isNullOrEmpty();
  }
}
