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
