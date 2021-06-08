package io.harness.beans.alerts;

import io.harness.event.QueryAlertCategory;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AlertMetadata {
  QueryAlertCategory alertCategory;
  AlertInfo alertInfo;
}
