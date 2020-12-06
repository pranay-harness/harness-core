package io.harness.ccm.cluster.entities;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TagFilter {
  String accountId;
  int limit;
  int offset;
  String tagName;
  String searchString;
}
