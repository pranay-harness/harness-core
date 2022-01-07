/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.graph.stepDetail;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stepDetail.StepDetailInstance;
import io.harness.category.element.UnitTests;
import io.harness.engine.observers.StepDetailsUpdateObserver;
import io.harness.observer.Subject;
import io.harness.pms.data.stepdetails.PmsStepDetails;
import io.harness.repositories.stepDetail.StepDetailsInstanceRepository;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsGraphStepDetailsServiceImplTest extends OrchestrationTestBase {
  @Mock private StepDetailsInstanceRepository stepDetailsInstanceRepository;
  @Mock private Subject<StepDetailsUpdateObserver> stepDetailsUpdateObserverSubject;

  @Inject @InjectMocks private PmsGraphStepDetailsServiceImpl pmsGraphStepDetailsService;

  @Before
  public void setUp() {
    Reflect.on(pmsGraphStepDetailsService).set("stepDetailsUpdateObserverSubject", stepDetailsUpdateObserverSubject);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void addStepDetail() {
    String nodeExecutionId = generateUuid();
    String planExecutionId = generateUuid();
    PmsStepDetails pmsStepDetails = new PmsStepDetails(new HashMap<>());
    String name = "name";
    when(stepDetailsInstanceRepository.save(any())).thenReturn(null);
    doNothing().when(stepDetailsUpdateObserverSubject).fireInform(any());

    pmsGraphStepDetailsService.addStepDetail(nodeExecutionId, planExecutionId, pmsStepDetails, name);

    verify(stepDetailsInstanceRepository).save(any());
    verify(stepDetailsUpdateObserverSubject).fireInform(any(), any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void getStepDetails() {
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();

    List<StepDetailInstance> instanceList = ImmutableList.of(
        StepDetailInstance.builder().stepDetails(PmsStepDetails.parse(new HashMap<>())).name("name").build());

    when(stepDetailsInstanceRepository.findByNodeExecutionId(nodeExecutionId)).thenReturn(instanceList);

    Map<String, PmsStepDetails> stepDetails =
        pmsGraphStepDetailsService.getStepDetails(planExecutionId, nodeExecutionId);

    assertThat(stepDetails).isNotNull();
    assertThat(stepDetails).isNotEmpty();
    assertThat(stepDetails.get("name")).isNotNull();

    PmsStepDetails pmsStepDetails = stepDetails.get("name");
    assertThat(pmsStepDetails).isEmpty();
  }
}
