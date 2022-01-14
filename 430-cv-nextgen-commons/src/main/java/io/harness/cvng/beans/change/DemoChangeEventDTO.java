package io.harness.cvng.beans.change;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DemoChangeEventDTO {
  ChangeSourceType changeSourceType;
  String changeSourceIdentifier;
  String monitoredServiceIdentifier;
}
