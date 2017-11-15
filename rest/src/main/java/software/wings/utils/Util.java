package software.wings.utils;

import software.wings.beans.NameValuePair;
import software.wings.exception.WingsException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author rktummala on 10/11/17
 */
public class Util {
  public static boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }

  public static boolean isNotEmpty(String value) {
    return value != null && !value.isEmpty();
  }

  public static boolean isEmpty(Collection collection) {
    return collection == null || collection.isEmpty();
  }

  public static String generatePath(String delimiter, boolean endsWithDelimiter, String... elements) {
    StringBuilder builder = new StringBuilder();
    for (String element : elements) {
      builder.append(element);
      builder.append(delimiter);
    }

    if (endsWithDelimiter) {
      return builder.toString();
    } else {
      return builder.substring(0, builder.length() - 1);
    }
  }

  public static List<NameValuePair> toYamlList(Map<String, Object> properties) {
    List<NameValuePair> nameValuePairs =
        properties.entrySet()
            .stream()
            .map(entry
                -> NameValuePair.builder()
                       .name(entry.getKey())
                       .value(entry.getValue() != null ? entry.getValue().toString() : null)
                       .build())
            .collect(Collectors.toList());
    return nameValuePairs;
  }

  public static Map<String, Object> toProperties(List<NameValuePair> nameValuePairList) {
    return nameValuePairList.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
  }

  public static <T extends Enum<T>> T getEnumFromString(Class<T> enumClass, String stringValue) {
    if (enumClass != null && stringValue != null) {
      try {
        return Enum.valueOf(enumClass, stringValue.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
        throw new WingsException(ex);
      }
    }
    return null;
  }

  public static String normalize(String input) {
    return input.replace('/', '_');
  }

  public static String getStringFromEnum(Enum enumObject) {
    if (enumObject != null) {
      return enumObject.name();
    }
    return null;
  }
}
