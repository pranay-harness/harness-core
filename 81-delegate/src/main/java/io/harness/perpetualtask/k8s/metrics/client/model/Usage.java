package io.harness.perpetualtask.k8s.metrics.client.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Usage implements Serializable {
  String cpu;
  String memory;
}
