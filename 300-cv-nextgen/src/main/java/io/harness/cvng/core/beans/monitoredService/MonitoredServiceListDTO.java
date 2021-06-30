package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.beans.MonitoredServiceType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class MonitoredServiceListDTO {
  String name;
  String identifier;
  String serviceRef;
  String environmentRef;
  MonitoredServiceType type;
}
