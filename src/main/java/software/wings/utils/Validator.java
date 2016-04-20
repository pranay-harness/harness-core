package software.wings.utils;

import software.wings.beans.ErrorConstants;
import software.wings.exception.WingsException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Validator {
  public static void notNullCheck(String name, Object value) {
    if (value == null) {
      Map<String, Object> map = new HashMap<>();
      map.put("name", name);
      map.put("value", value);
      throw new WingsException(map, ErrorConstants.INVALID_ARGUMENT);
    }
  }

  public static void equalCheck(Object applicationId, Object uuid) {
    if (!Objects.equals(applicationId, uuid)) {
      Map<String, Object> map = new HashMap<>();
      map.put("applicationId", applicationId);
      map.put("uuid", uuid);
      throw new WingsException(map, ErrorConstants.INVALID_ARGUMENT);
    }
  }
}
