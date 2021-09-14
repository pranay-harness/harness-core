/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.spotinst.response;

import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.spotinst.model.ElastiGroup;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpotinstTrafficShiftAlbSetupResponse implements SpotInstTaskResponse {
  private ElastiGroup newElastigroup;
  private List<ElastiGroup> elastiGroupsToBeDownsized;
  private List<LbDetailsForAlbTrafficShift> lbDetailsWithTargetGroups;
}
