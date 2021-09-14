/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class CustomDataCollectionUtils {
  private static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  private static final Pattern DOLLAR_REF_REGEX = Pattern.compile("\\$\\{(.*?)}");

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

  public static String resolveDollarReferences(String input, Map<String, String> replacements) {
    StringBuilder stringBuilder = new StringBuilder();
    Matcher matcher = DOLLAR_REF_REGEX.matcher(input);
    int start = 0;
    while (matcher.find()) {
      String dollarVariableKey = matcher.group(1);
      if (replacements.containsKey(dollarVariableKey)) {
        stringBuilder.append(input, start, matcher.start());
        stringBuilder.append(replacements.get(dollarVariableKey));
        start = matcher.end();
      }
    }
    stringBuilder.append(input, start, input.length());
    return stringBuilder.toString();
  }
}
