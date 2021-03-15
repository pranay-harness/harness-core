package io.harness.pms.approval.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApproverInput {
  String name;
  String value;
}
