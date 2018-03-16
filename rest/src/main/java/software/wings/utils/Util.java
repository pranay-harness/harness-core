package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.NameValuePair;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.NameValuePairYamlHandler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author rktummala on 10/11/17
 */
public class Util {
  private static final Logger logger = LoggerFactory.getLogger(Util.class);

  private static final String FIRST_REVISION = ".1";

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

  public static List<NameValuePair.Yaml> toNameValuePairYamlList(
      Map<String, Object> properties, String appId, NameValuePairYamlHandler nameValuePairYamlHandler) {
    return properties.entrySet()
        .stream()
        .map(entry -> {
          NameValuePair nameValuePair = NameValuePair.builder()
                                            .name(entry.getKey())
                                            .value(entry.getValue() != null ? entry.getValue().toString() : null)
                                            .build();
          return nameValuePairYamlHandler.toYaml(nameValuePair, appId);
        })
        .collect(Collectors.toList());
  }

  public static Map<String, Object> toProperties(List<NameValuePair> nameValuePairList) {
    // do not use Collectors.toMap, as it throws NPE if any of the value is null
    // here we do expect value to be null in some cases.
    return nameValuePairList.stream().collect(
        HashMap::new, (m, v) -> m.put(v.getName(), v.getValue()), HashMap::putAll);
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
          logger.error("", ex);
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

  public static Type[] getParameterizedTypes(Object object) {
    Type superclassType = object.getClass().getGenericSuperclass();
    if (!ParameterizedType.class.isAssignableFrom(superclassType.getClass())) {
      return null;
    }
    return ((ParameterizedType) superclassType).getActualTypeArguments();
  }
}
