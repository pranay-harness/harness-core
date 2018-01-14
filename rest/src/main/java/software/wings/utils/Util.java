package software.wings.utils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
  private static final String FIRST_REVISION = ".1";

  // TODO: remove this helper method or commit to it everywhere
  public static boolean isEmpty(String value) {
    return StringUtils.isEmpty(value);
  }

  // TODO: remove this helper method or commit to it everywhere
  public static boolean isNotEmpty(String value) {
    return StringUtils.isNotEmpty(value);
  }

  // TODO: remove this helper method or commit to it everywhere
  public static boolean isEmpty(Collection collection) {
    return CollectionUtils.isEmpty(collection);
  }

  // TODO: remove this helper method or commit to it everywhere
  public static boolean isNotEmpty(Collection collection) {
    return CollectionUtils.isNotEmpty(collection);
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
    return properties.entrySet()
        .stream()
        .map(entry
            -> NameValuePair.builder()
                   .name(entry.getKey())
                   .value(entry.getValue() != null ? entry.getValue().toString() : null)
                   .build())
        .collect(Collectors.toList());
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

  /**
   * This method gets the default name, checks if another entry exists with the same name, if exists, it parses and
   * extracts the revision and creates a name with the next revision.
   *
   * @param existingName
   * @param defaultName
   */
  public static String getNameWithNextRevision(String existingName, String defaultName) {
    String name = defaultName;

    if (isEmpty(existingName)) {
      return name;
    }

    int index = existingName.lastIndexOf('.');
    if (index == -1) {
      name = name + FIRST_REVISION;
    } else {
      if (index < existingName.length() - 1) {
        String revisionString = existingName.substring(index + 1);
        int revision = -1;
        try {
          revision = Integer.parseInt(revisionString);
        } catch (NumberFormatException ex) {
        }

        if (revision != -1) {
          revision++;
          name = name + "." + revision;
        } else {
          name = name + FIRST_REVISION;
        }
      } else {
        name = name + FIRST_REVISION;
      }
    }
    return name;
  }
}
