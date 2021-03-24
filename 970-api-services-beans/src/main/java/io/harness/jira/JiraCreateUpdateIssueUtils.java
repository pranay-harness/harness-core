package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class JiraCreateUpdateIssueUtils {
  public void updateFieldValues(
      Map<String, Object> currFieldValues, Map<String, JiraFieldNG> issueTypeFields, Map<String, String> fields) {
    if (EmptyPredicate.isEmpty(issueTypeFields) || EmptyPredicate.isEmpty(fields)) {
      return;
    }

    Set<String> fieldKeys = Sets.intersection(issueTypeFields.keySet(), fields.keySet());

    // Remove time tracking user facing fields from fieldKeys. If they are present do special handling for them.
    fieldKeys.remove(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME);
    fieldKeys.remove(JiraConstantsNG.REMAINING_ESTIMATE_NAME);
    addTimeTrackingField(currFieldValues, issueTypeFields, fields);

    fieldKeys.forEach(key -> addKey(currFieldValues, key, issueTypeFields.get(key), fields.get(key)));
  }

  private void addTimeTrackingField(
      Map<String, Object> currFieldValues, Map<String, JiraFieldNG> issueTypeFields, Map<String, String> fields) {
    // Check if there is timetracking field in schema or not.
    Optional<String> timeTrackingKey = issueTypeFields.values()
                                           .stream()
                                           .filter(f -> f.getSchema().getType() == JiraFieldTypeNG.TIME_TRACKING)
                                           .findFirst()
                                           .map(JiraFieldNG::getKey);
    if (!timeTrackingKey.isPresent()) {
      return;
    }

    String originalEstimate = fields.get(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME);
    String remainingEstimate = fields.get(JiraConstantsNG.REMAINING_ESTIMATE_NAME);
    if (EmptyPredicate.isEmpty(originalEstimate) && EmptyPredicate.isEmpty(remainingEstimate)) {
      return;
    }

    currFieldValues.put(timeTrackingKey.get(), new JiraTimeTrackingFieldNG(originalEstimate, remainingEstimate));
  }

  private void addKey(Map<String, Object> currFieldValues, String key, JiraFieldNG field, String value) {
    if (key == null || field == null || EmptyPredicate.isEmpty(value)) {
      return;
    }

    if (!field.getSchema().isArray()) {
      Object finalValue = convertToFinalValue(field, value);
      if (finalValue != null) {
        currFieldValues.put(field.getKey(), convertToFinalValue(field, value));
      }
    }

    List<String> values = new ArrayList<>();
    for (String s : Splitter.on(JiraConstantsNG.COMMA_SPLIT_PATTERN).trimResults().split(value)) {
      if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1) {
        String str = s.substring(1, s.length() - 1).trim();
        values.add(str.replaceAll("\"\"", "\""));
      } else {
        values.add(s);
      }
    }

    currFieldValues.put(field.getKey(),
        values.stream().map(v -> convertToFinalValue(field, v)).filter(Objects::nonNull).collect(Collectors.toList()));
  }

  private Object convertToFinalValue(JiraFieldNG field, String value) {
    switch (field.getSchema().getType()) {
      case STRING:
        return value;
      case NUMBER:
        Long lVal = tryParseLong(value);
        if (lVal != null) {
          return lVal;
        }
        return tryParseDouble(value);
      case DATE:
        return tryParseDate(value);
      case DATETIME:
        return tryParseDateTime(value);
      case OPTION:
        return convertOptionToFinalValue(field, value);
      default:
        return null;
    }
  }

  private Object convertOptionToFinalValue(JiraFieldNG field, String value) {
    if (EmptyPredicate.isEmpty(field.getAllowedValues())) {
      return null;
    }
    return field.getAllowedValues().stream().filter(av -> av.matchesValue(value)).findFirst().orElse(null);
  }

  private Long tryParseLong(String str) {
    try {
      return Long.parseLong(str);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private Double tryParseDouble(String str) {
    try {
      return Double.parseDouble(str);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private String tryParseDate(String str) {
    for (DateTimeFormatter formatter : JiraConstantsNG.DATE_FORMATTERS) {
      try {
        return LocalDate.parse(str, formatter).format(JiraConstantsNG.DATE_FORMATTER);
      } catch (NumberFormatException ignored) {
        // ignored
      }
    }
    return null;
  }

  private String tryParseDateTime(String str) {
    for (DateTimeFormatter formatter : JiraConstantsNG.DATETIME_FORMATTERS) {
      try {
        return ZonedDateTime.parse(str, formatter).format(JiraConstantsNG.DATETIME_FORMATTER);
      } catch (NumberFormatException ignored) {
        // ignored
      }
    }
    return null;
  }
}
