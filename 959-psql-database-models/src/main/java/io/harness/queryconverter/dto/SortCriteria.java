package io.harness.queryconverter.dto;

import lombok.Getter;

@Getter
public class SortCriteria {
  String field;
  SortOrder order = SortOrder.ASCENDING;
}
