package io.harness.pms.sdk.core.execution.expression;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class ExpressionResultUtils {
  public static final String STRING = "String";
  public static final String BOOLEAN = "Boolean";
  public static final String INTEGER = "Integer";
  public static final String BYTES = "Bytes";

  public static Map<Class, String> primitivesMap = new HashMap() {
    {
      put(String.class.getSimpleName(), STRING);
      put(Integer.class.getSimpleName(), INTEGER);
      put(Boolean.class.getSimpleName(), BOOLEAN);
      put(Byte.class.getSimpleName(), BYTES);
    }
  };

  public static Object getPrimitiveResponse(String value, String clazz) {
    switch (ExpressionResultUtils.primitivesMap.get(clazz)) {
      case ExpressionResultUtils.INTEGER:
        return Integer.parseInt(value);
      case ExpressionResultUtils.BOOLEAN:
        return Boolean.parseBoolean(value);
      case ExpressionResultUtils.BYTES:
        return Byte.valueOf(value);
      default:
        return value;
    }
  }
}
