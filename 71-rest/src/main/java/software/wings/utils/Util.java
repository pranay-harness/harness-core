package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePair.Yaml;
import software.wings.service.impl.yaml.handler.NameValuePairYamlHandler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author rktummala on 10/11/17
 */
public class Util {
  private static final Logger logger = LoggerFactory.getLogger(Util.class);

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
        .collect(toList());
  }

  public static Map<String, Object> toProperties(List<NameValuePair> nameValuePairList) {
    // do not use Collectors.toMap, as it throws NPE if any of the value is null
    // here we do expect value to be null in some cases.
    return nameValuePairList.stream().collect(
        HashMap::new, (m, v) -> m.put(v.getName(), v.getValue()), HashMap::putAll);
  }

  public static List<NameValuePair.Yaml> getSortedNameValuePairYamlList(List<NameValuePair.Yaml> yamlList) {
    if (isEmpty(yamlList)) {
      return yamlList;
    }

    return yamlList.stream()
        .sorted(new Comparator<Yaml>() {
          @Override
          public int compare(Yaml o1, Yaml o2) {
            return o1.getName().compareTo(o2.getName());
          }
        })
        .collect(toList());
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
   */
  public static String getNameWithNextRevision(List<String> existingNames, String defaultName) {
    String existingName = "";
    int maxRevision = -1;
    for (String existing : existingNames) {
      int revision;
      if (existing.equals(defaultName)) {
        revision = 0;
      } else {
        String rev = existing.substring(defaultName.length() + 1);
        try {
          revision = Integer.parseInt(rev);
        } catch (NumberFormatException ex) {
          revision = -1;
        }
      }
      if (revision > maxRevision) {
        maxRevision = revision;
        existingName = existing;
      }
    }

    if (isBlank(existingName)) {
      return defaultName;
    }

    int revision = maxRevision + 1;
    return defaultName + "-" + revision;
  }

  public static Type[] getParameterizedTypes(Object object) {
    Type superclassType = object.getClass().getGenericSuperclass();
    if (!ParameterizedType.class.isAssignableFrom(superclassType.getClass())) {
      return null;
    }
    return ((ParameterizedType) superclassType).getActualTypeArguments();
  }

  public static String escapifyString(String input) {
    String str = input.replaceAll("`", "\\\\`").replaceAll("\"", "\\\\\"");

    if (str.endsWith("\\")) {
      str = str.substring(0, str.length() - 1) + "\\\\";
    }
    return str;
  }
}
