package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;

import com.google.common.base.Splitter;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class JiraIssueUtilsNG {
  public List<String> splitByComma(String value) {
    List<String> values = new ArrayList<>();
    if (isBlank(value)) {
      return values;
    }

    for (String s : Splitter.on(JiraConstantsNG.COMMA_SPLIT_PATTERN).trimResults().split(value)) {
      if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1) {
        String str = s.substring(1, s.length() - 1).trim();
        values.add(str.replaceAll("\"\"", "\""));
      } else {
        values.add(s);
      }
    }
    return values;
  }

  public void updateFieldValues(
      Map<String, Object> currFieldValues, Map<String, JiraFieldNG> issueTypeFields, Map<String, String> fields) {
    if (EmptyPredicate.isEmpty(issueTypeFields) || EmptyPredicate.isEmpty(fields)) {
      return;
    }

    Set<String> invalidFields =
        fields.keySet().stream().filter(k -> !issueTypeFields.containsKey(k)).collect(Collectors.toSet());
    if (EmptyPredicate.isNotEmpty(invalidFields)) {
      throw new InvalidRequestException(String.format("Some fields are invalid: %s", String.join(", ", invalidFields)));
    }

    Set<String> fieldKeys = new HashSet<>(fields.keySet());

    // Remove time tracking user facing fields from fieldKeys. If they are present do special handling for them.
    fieldKeys.remove(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME);
    fieldKeys.remove(JiraConstantsNG.REMAINING_ESTIMATE_NAME);
    addTimeTrackingField(currFieldValues, issueTypeFields, fields);

    fieldKeys.forEach(key -> addKey(currFieldValues, key, issueTypeFields.get(key), fields.get(key)));
  }

  private void addTimeTrackingField(
      Map<String, Object> currFieldValues, Map<String, JiraFieldNG> issueTypeFields, Map<String, String> fields) {
    String originalEstimate = fields.get(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME);
    String remainingEstimate = fields.get(JiraConstantsNG.REMAINING_ESTIMATE_NAME);
    if (EmptyPredicate.isEmpty(originalEstimate) && EmptyPredicate.isEmpty(remainingEstimate)) {
      return;
    }

    currFieldValues.put(
        JiraConstantsNG.TIME_TRACKING_KEY, new JiraTimeTrackingFieldNG(originalEstimate, remainingEstimate));
  }

  private void addKey(Map<String, Object> currFieldValues, String key, JiraFieldNG field, String value) {
    if (key == null || field == null || EmptyPredicate.isEmpty(value)) {
      return;
    }

    if (!field.getSchema().isArray()) {
      Object finalValue = convertToFinalValue(field, key, value);
      if (finalValue != null) {
        currFieldValues.put(field.getKey(), finalValue);
      }
      return;
    }

    List<String> values = splitByComma(value);
    currFieldValues.put(field.getKey(),
        values.stream()
            .map(v -> convertToFinalValue(field, key, v))
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
  }

  private Object convertToFinalValue(JiraFieldNG field, String name, String value) {
    switch (field.getSchema().getType()) {
      case STRING:
        return value;
      case NUMBER:
        Long lVal = tryParseLong(value);
        if (lVal != null) {
          return lVal;
        }
        return parseDouble(name, value);
      case DATE:
        return parseDate(name, value);
      case DATETIME:
        return tryParseDateTime(name, value);
      case OPTION:
        Object optionValue = convertOptionToFinalValue(field, value);
        if (optionValue == null) {
          throw new InvalidRequestException(String.format("Field [%s] value is not in the allowed values", name));
        }
        return optionValue;
      default:
        throw new InvalidRequestException(String.format("Unsupported field type: %s", field.getSchema().getType()));
    }
  }

  private Object convertOptionToFinalValue(JiraFieldNG field, String value) {
    if (EmptyPredicate.isEmpty(field.getAllowedValues())) {
      return null;
    }
    return field.getAllowedValues().stream().filter(av -> av.matchesValue(value)).findFirst().orElse(null);
  }

  private Long tryParseLong(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private Double parseDouble(String name, String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException ignored) {
      throw new InvalidRequestException(String.format("Field [%s] expects a number value", name));
    }
  }

  private String parseDate(String name, String value) {
    for (DateTimeFormatter formatter : JiraConstantsNG.DATE_FORMATTERS) {
      try {
        return LocalDate.parse(value, formatter).format(JiraConstantsNG.DATE_FORMATTER);
      } catch (NumberFormatException ignored) {
        // ignored
      }
    }
    throw new InvalidRequestException(String.format("Field [%s] expects a date value", name));
  }

  private String tryParseDateTime(String name, String value) {
    for (DateTimeFormatter formatter : JiraConstantsNG.DATETIME_FORMATTERS) {
      try {
        return ZonedDateTime.parse(value, formatter).format(JiraConstantsNG.DATETIME_FORMATTER);
      } catch (NumberFormatException ignored) {
        // ignored
      }
    }
    throw new InvalidRequestException(String.format("Field [%s] expects a datetime value", name));
  }
}
