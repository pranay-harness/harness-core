package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.util.Collections.singletonMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.jira.deserializer.JiraIssueTypeDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = JiraIssueTypeDeserializer.class)
public class JiraCreateIssueRequestNG {
  @NotNull Map<String, Object> fields = new HashMap<>();

  public JiraCreateIssueRequestNG(JiraProjectNG project, JiraIssueTypeNG issueType, Map<String, String> fields) {
    // Add project and issue type fields which are required and are not part of fields. We don't need special handling
    // for status field as we manually remove status field from issueType.fields before calling this method.
    this.fields.put(JiraConstantsNG.PROJECT_KEY, singletonMap("key", project.getKey()));
    this.fields.put(JiraConstantsNG.ISSUE_TYPE_KEY, singletonMap("id", issueType.getId()));

    if (EmptyPredicate.isEmpty(fields)) {
      return;
    }

    Map<String, JiraFieldNG> issueTypeFields = issueType.getFields();
    Set<String> fieldKeys = Sets.intersection(issueTypeFields.keySet(), fields.keySet());

    // Remove time tracking user facing fields from fieldKeys. If they are present do special handling for them.
    fieldKeys.remove(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME);
    fieldKeys.remove(JiraConstantsNG.REMAINING_ESTIMATE_NAME);
    addTimeTrackingField(issueType, fields);

    fieldKeys.forEach(key -> addKey(key, issueTypeFields.get(key), fields.get(key)));
  }

  private void addTimeTrackingField(JiraIssueTypeNG issueType, Map<String, String> fields) {
    // Check if there is timetracking field in schema or not.
    Optional<String> timeTrackingKey = issueType.getFields()
                                           .values()
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

    this.fields.put(timeTrackingKey.get(), new JiraTimeTrackingFieldNG(originalEstimate, remainingEstimate));
  }

  private void addKey(String key, JiraFieldNG field, String value) {
    if (key == null || field == null || EmptyPredicate.isEmpty(value)) {
      return;
    }

    if (!field.getSchema().isArray()) {
      Object finalValue = convertToFinalValue(field, value);
      if (finalValue != null) {
        fields.put(field.getKey(), convertToFinalValue(field, value));
      }
    }

    List<String> values = JiraIssueUtils.splitByComma(value);
    fields.put(field.getKey(),
        values.stream().map(v -> convertToFinalValue(field, v)).filter(Objects::nonNull).collect(Collectors.toList()));
  }

  private static Object convertToFinalValue(JiraFieldNG field, String value) {
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

  private static Object convertOptionToFinalValue(JiraFieldNG field, String value) {
    if (EmptyPredicate.isEmpty(field.getAllowedValues())) {
      return null;
    }
    return field.getAllowedValues().stream().filter(av -> av.matchesValue(value)).findFirst().orElse(null);
  }

  private static Long tryParseLong(String str) {
    try {
      return Long.parseLong(str);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private static Double tryParseDouble(String str) {
    try {
      return Double.parseDouble(str);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private static String tryParseDate(String str) {
    for (DateTimeFormatter formatter : JiraConstantsNG.DATE_FORMATTERS) {
      try {
        return LocalDate.parse(str, formatter).format(JiraConstantsNG.DATE_FORMATTER);
      } catch (NumberFormatException ignored) {
        // ignored
      }
    }
    return null;
  }

  private static String tryParseDateTime(String str) {
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
