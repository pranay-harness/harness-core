package io.harness.event.reconciliation.deployment;

public enum DetectionStatus {
  SUCCESS, /*No issues found*/
  MISSING_RECORDS_DETECTED, /*There were some records missing in the timeScaleDB*/
  DUPLICATE_DETECTED, /*There were some records that were found to be duplicate in timeScaleDB*/
  DUPLICATE_DETECTED_MISSING_RECORDS_DETECTED, /*Some records were missing and some duplicates were found*/
  ERROR /*Detection failed*/
}
