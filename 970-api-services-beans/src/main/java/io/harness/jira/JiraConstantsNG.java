package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@OwnedBy(CDC)
public interface JiraConstantsNG {
  DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  DateTimeFormatter[] DATE_FORMATTERS = {DATE_FORMATTER, DateTimeFormatter.ISO_LOCAL_DATE};

  DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  DateTimeFormatter[] DATETIME_FORMATTERS = {DATETIME_FORMATTER, DateTimeFormatter.ISO_DATE_TIME,
      DateTimeFormatter.ISO_OFFSET_DATE_TIME, DateTimeFormatter.ISO_ZONED_DATE_TIME};

  Pattern COMMA_SPLIT_PATTERN = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

  // Special fields
  String PROJECT_KEY = "project";
  String PROJECT_INTERNAL_NAME = "__" + PROJECT_KEY;

  String ISSUE_TYPE_KEY = "issuetype";
  String ISSUE_TYPE_INTERNAL_NAME = "__" + ISSUE_TYPE_KEY;

  String STATUS_KEY = "status";
  String STATUS_NAME = "Status";
  String STATUS_INTERNAL_NAME = "__" + STATUS_KEY;

  String TIME_TRACKING_KEY = "timetracking";
  String ORIGINAL_ESTIMATE_NAME = "Original Estimate";
  String REMAINING_ESTIMATE_NAME = "Remaining Estimate";
  String ORIGINAL_ESTIMATE_KEY = "originalEstimate";
  String REMAINING_ESTIMATE_KEY = "remainingEstimate";
}
