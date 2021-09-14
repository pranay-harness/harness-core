/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instana;

import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InstanaSetupTestNodeData extends SetupTestNodeData {
  private InstanaInfraParams infraParams;
  private InstanaApplicationParams applicationParams;
  private List<InstanaTagFilter> tagFilters;
  @Builder
  private InstanaSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      Instance instanceElement, String hostExpression, String workflowId, long fromTime, long toTime, String guid,
      InstanaInfraParams infraParams, InstanaApplicationParams applicationParams, List<InstanaTagFilter> tagFilters) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.INSTANA, fromTime, toTime);
    this.infraParams = infraParams;
    this.applicationParams = applicationParams;
    this.tagFilters = tagFilters;
  }

  public List<InstanaTagFilter> getTagFilters() {
    if (tagFilters == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(this.tagFilters);
  }
}
