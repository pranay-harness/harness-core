package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomPayloadExpression {
  String expression;
  String value;
}
