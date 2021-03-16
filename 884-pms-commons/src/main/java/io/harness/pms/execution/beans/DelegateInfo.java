package io.harness.pms.execution.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DelegateInfo {
  String id;
  String name;
}
