/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import software.wings.beans.InstanceUnitType;
import software.wings.sm.StepExecutionSummary;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 12/22/17.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class AmiStepExecutionSummary extends StepExecutionSummary {
  private int instanceCount;
  private InstanceUnitType instanceUnitType;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;

  public AmiServiceDeployElement getRollbackAmiServiceElement() {
    return AmiServiceDeployElement.builder()
        .instanceCount(instanceCount)
        .instanceUnitType(instanceUnitType)
        .newInstanceData(newInstanceData)
        .oldInstanceData(oldInstanceData)
        .build();
  }
}
