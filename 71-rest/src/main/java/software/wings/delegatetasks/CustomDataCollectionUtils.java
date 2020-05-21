package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
@Slf4j
public class CustomDataCollectionUtils {
  private static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

  public static String resolveField(String string, String fieldToResolve, String value) {
    if (isEmpty(string)) {
      return string;
    }
    String result = string;
    if (result.contains(fieldToResolve)) {
      result = result.replace(fieldToResolve, value);
    }
    return result;
  }

  public static String resolvedUrl(String url, String host, long startTime, long endTime, String query) {
    String result = url;
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat(ISO_DATE_FORMAT);
    df.setTimeZone(tz);

    if (result.contains("${start_time}")) {
      result = result.replace("${start_time}", String.valueOf(startTime));
    }
    if (result.contains("${end_time}")) {
      result = result.replace("${end_time}", String.valueOf(endTime));
    }
    if (result.contains("${iso_start_time}")) {
      String startIso = df.format(startTime);
      result = result.replace("${iso_start_time}", startIso);
    }
    if (result.contains("${iso_end_time}")) {
      String endIso = df.format(endTime);
      result = result.replace("${iso_end_time}", endIso);
    }
    if (result.contains("${start_time_seconds}")) {
      result = result.replace("${start_time_seconds}", String.valueOf(startTime / 1000L));
    }
    if (result.contains("${end_time_seconds}")) {
      result = result.replace("${end_time_seconds}", String.valueOf(endTime / 1000L));
    }
    if (result.contains("${query}")) {
      result = result.replace("${query}", query);
    }
    if (result.contains("${host}")) {
      result = result.replace("${host}", host);
    }
    return result;
  }

  public static Map<String, Object> resolveMap(
      Map<String, Object> input, String host, long startTime, long endTime, String query) {
    Map<String, Object> resolvedMap = new HashMap<>();
    if (input == null) {
      return resolvedMap;
    }
    input.forEach((key, value) -> {
      Object resolvedObject = null;
      if (value instanceof Map) {
        resolvedObject = resolveMap((Map) value, host, startTime, endTime, query);
      } else if (value instanceof String) {
        resolvedObject = resolvedUrl((String) value, host, startTime, endTime, query);
      }
      resolvedMap.put(key, resolvedObject);
    });
    return resolvedMap;
  }

  public static String getMaskedString(String stringToMask, String matcherPattern, List<String> stringsToReplace) {
    Pattern batchPattern = Pattern.compile(matcherPattern);
    Matcher matcher = batchPattern.matcher(stringToMask);
    while (matcher.find()) {
      for (int i = 0; i < stringsToReplace.size() && i < matcher.groupCount(); i++) {
        final String subStringToReplace = matcher.group(i + 1);
        stringToMask = stringToMask.replace(subStringToReplace, stringsToReplace.get(i));
      }
    }
    return stringToMask;
  }

  public static String getConcatenatedQuery(Set<String> queries, String separator) {
    String concatenatedQuery = null;
    if (isNotEmpty(queries) && isNotEmpty(separator)) {
      StringBuilder hostQueryBuilder = new StringBuilder();
      queries.forEach(host -> {
        if (!hostQueryBuilder.toString().isEmpty()) {
          hostQueryBuilder.append(separator);
        }
        hostQueryBuilder.append(host);
      });
      concatenatedQuery = hostQueryBuilder.toString();
    } else if (isNotEmpty(queries) && queries.size() == 1) {
      concatenatedQuery = new ArrayList<>(queries).get(0);
    } else {
      logger.error("Incorrect combination of query and separator");
    }
    return concatenatedQuery;
  }
}
