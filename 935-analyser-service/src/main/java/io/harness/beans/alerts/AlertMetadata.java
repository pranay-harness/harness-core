/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.alerts;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.QueryAlertCategory;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
@FieldNameConstants(innerTypeName = "AlertMetadataKeys")
public class AlertMetadata {
  QueryAlertCategory alertCategory;
  AlertInfo alertInfo;
}
