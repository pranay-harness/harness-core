package io.harness.beans.alerts;

import io.harness.event.QueryAlertCategory;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "AlertMetadataKeys")
public class AlertMetadata {
  QueryAlertCategory alertCategory;
  AlertInfo alertInfo;
}
