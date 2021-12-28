package io.harness.cvng.beans;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CV)
public enum DataCollectionRequestType {
  SPLUNK_SAVED_SEARCHES,
  SPLUNK_SAMPLE_DATA,
  SPLUNK_LATEST_HISTOGRAM,
  STACKDRIVER_DASHBOARD_LIST,
  STACKDRIVER_DASHBOARD_GET,
  STACKDRIVER_SAMPLE_DATA,
  STACKDRIVER_LOG_SAMPLE_DATA,
  APPDYNAMICS_FETCH_APPS,
  APPDYNAMICS_FETCH_TIERS,
  APPDYNAMICS_GET_METRIC_DATA,
  APPDYNAMICS_GET_SINGLE_METRIC_DATA,
  APPDYNAMICS_FETCH_METRIC_STRUCTURE,
  NEWRELIC_APPS_REQUEST,
  NEWRELIC_VALIDATION_REQUEST,
  PROMETHEUS_METRIC_LIST_GET,
  PROMETHEUS_LABEL_NAMES_GET,
  PROMETHEUS_LABEL_VALUES_GET,
  PROMETHEUS_SAMPLE_DATA,
  PAGERDUTY_SERVICES,
  PAGERDUTY_REGISTER_WEBHOOK,
  PAGERDUTY_DELETE_WEBHOOK,
  DATADOG_DASHBOARD_LIST,
  DATADOG_DASHBOARD_DETAILS,
  DATADOG_METRIC_TAGS,
  DATADOG_ACTIVE_METRICS,
  DATADOG_TIME_SERIES_POINTS,
  DATADOG_LOG_SAMPLE_DATA,
  DATADOG_LOG_INDEXES,
  NEWRELIC_SAMPLE_FETCH_REQUEST,
  SYNC_DATA_COLLECTION,
  CUSTOM_HEALTH_SAMPLE_DATA,
  DYNATRACE_SERVICE_LIST,
  DYNATRACE_SERVICE_DETAILS
}
