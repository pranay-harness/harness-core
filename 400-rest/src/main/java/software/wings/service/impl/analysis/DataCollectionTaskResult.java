/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.sm.StateType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCollectionTaskResult implements DelegateTaskNotifyResponseData {
  private DataCollectionTaskStatus status;
  private String errorMessage;
  private StateType stateType;
  private DelegateMetaInfo delegateMetaInfo;

  // State specific results.....

  // NewRelicDeploymentMarker state
  private String newRelicDeploymentMarkerBody;

  public enum DataCollectionTaskStatus {
    /**
     * Success execution status.
     */
    SUCCESS,
    /**
     * Failure execution status.
     */
    FAILURE;
  }
}
