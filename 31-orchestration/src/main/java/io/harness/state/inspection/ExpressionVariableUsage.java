package io.harness.state.inspection;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ExpressionVariableUsage implements StateInspectionData {
  @Value
  @Builder
  public static class Item {
    private String expression;
    private String value;
    private int count;
  }

  private List<Item> variables;

  @Override
  public String key() {
    return "expressionVariableUsage";
  }
}
