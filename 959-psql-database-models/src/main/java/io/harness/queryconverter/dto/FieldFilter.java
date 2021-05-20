package io.harness.queryconverter.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FieldFilter implements GraphQLFilter {
  String field;
  List<String> values;
  FilterOperator operator;
}