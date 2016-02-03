package software.wings.utils;

import java.util.HashMap;
import java.util.Map;

import software.wings.beans.ErrorConstants;
import software.wings.exception.WingsException;

public class Validator {
  public static void notNullCheck(String name, Object value) {
    if (value == null) {
      Map<String, Object> map = new HashMap<>();
      map.put("name", name);
      map.put("value", value);
      throw new WingsException(map, ErrorConstants.INVALID_ARGUMENT);
    }
  }

  public static void equalCheck(Object applicationId, Object uuid) {}
}
