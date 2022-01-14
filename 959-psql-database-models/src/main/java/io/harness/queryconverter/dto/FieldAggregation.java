package io.harness.queryconverter.dto;

import lombok.Getter;

@Getter
public class FieldAggregation {
  String field;
  AggregationOperation operation = AggregationOperation.SUM;
}
