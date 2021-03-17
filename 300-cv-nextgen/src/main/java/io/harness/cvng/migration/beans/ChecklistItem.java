package io.harness.cvng.migration.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChecklistItem {
  public static ChecklistItem NA = ChecklistItem.builder().desc("N/A").build();
  String desc;
}
