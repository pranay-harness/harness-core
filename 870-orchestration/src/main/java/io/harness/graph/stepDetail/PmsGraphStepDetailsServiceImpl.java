/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.graph.stepDetail;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stepDetail.StepDetailInstance;
import io.harness.engine.observers.StepDetailsUpdateInfo;
import io.harness.engine.observers.StepDetailsUpdateObserver;
import io.harness.graph.stepDetail.service.PmsGraphStepDetailsService;
import io.harness.observer.Subject;
import io.harness.pms.data.stepdetails.PmsStepDetails;
import io.harness.repositories.stepDetail.StepDetailsInstanceRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PmsGraphStepDetailsServiceImpl implements PmsGraphStepDetailsService {
  @Inject StepDetailsInstanceRepository stepDetailsInstanceRepository;
  @Inject @Getter private final Subject<StepDetailsUpdateObserver> stepDetailsUpdateObserverSubject = new Subject<>();

  @Override
  public void addStepDetail(String nodeExecutionId, String planExecutionId, PmsStepDetails stepDetails, String name) {
    StepDetailInstance stepDetailInstance = StepDetailInstance.builder()
                                                .name(name)
                                                .stepDetails(stepDetails)
                                                .planExecutionId(planExecutionId)
                                                .nodeExecutionId(nodeExecutionId)
                                                .build();
    stepDetailsInstanceRepository.save(stepDetailInstance);
    stepDetailsUpdateObserverSubject.fireInform(StepDetailsUpdateObxxxxxxxx:onStepDetailsUpdate,
        StepDetailsUpdateInfo.builder().nodeExecutionId(nodeExecutionId).planExecutionId(planExecutionId).build());
  }

  @Override
  public Map<String, PmsStepDetails> getStepDetails(String planExecutionId, String nodeExecutionId) {
    List<StepDetailInstance> stepDetailInstances = stepDetailsInstanceRepository.findByNodeExecutionId(nodeExecutionId);
    return stepDetailInstances.stream().collect(
        Collectors.toMap(StepDetailInstance::getName, StepDetailInstance::getStepDetails));
  }
}
