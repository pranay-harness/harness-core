/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.sm.StateType;

import com.google.api.client.util.Lists;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Common Log Data Collection Info class containing attributes used by Log Verification providers while
 * Data collection
 * Created by rsingh on 8/8/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class LogDataCollectionInfo
    extends DataCollectionInfo implements TaskParameters, ExecutionCapabilityDemander {
  private String query;
  private long startTime;
  private long endTime;
  private int startMinute;
  private int collectionTime;
  private String hostnameField;
  private Set<String> hosts;
  private StateType stateType;
  private int initialDelayMinutes;
  List<EncryptedDataDetail> encryptedDataDetails;

  public LogDataCollectionInfo(String accountId, String applicationId, String stateExecutionId, String cvConfigId,
      String workflowId, String workflowExecutionId, String serviceId, String query, long startTime, long endTime,
      int startMinute, int collectionTime, String hostnameField, Set<String> hosts, StateType stateType,
      List<EncryptedDataDetail> encryptedDataDetails, int initialDelayMinutes) {
    super(accountId, applicationId, stateExecutionId, cvConfigId, workflowId, workflowExecutionId, serviceId);
    this.query = query;
    this.startTime = startTime;
    this.endTime = endTime;
    this.startMinute = startMinute;
    this.collectionTime = collectionTime;
    this.hostnameField = hostnameField;
    this.hosts = hosts;
    this.stateType = stateType;
    this.encryptedDataDetails = encryptedDataDetails;
    this.initialDelayMinutes = initialDelayMinutes;
  }

  public void copy(LogDataCollectionInfo clone) {
    super.copy(clone);
    clone.setQuery(this.getQuery());
    clone.setStartTime(this.getStartTime());
    clone.setEndTime(this.getEndTime());
    clone.setStartMinute(this.getStartMinute());
    clone.setCollectionTime(this.getCollectionTime());
    clone.setHostnameField(this.getHostnameField());
    clone.setHosts(hosts == null ? null : new HashSet(this.getHosts()));
    clone.setStateType(this.getStateType());
    clone.setEncryptedDataDetails(
        encryptedDataDetails == null ? null : Lists.newArrayList(this.getEncryptedDataDetails()));
    clone.setInitialDelayMinutes(this.getInitialDelayMinutes());
  }
}
