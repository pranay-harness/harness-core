package io.harness.event.reconciliation.deployment;

public enum DetectionStatus {
  SUCCESS, /*No issues found*/
  MISSING_RECORDS_DETECTED, /*There were some records missing in the timeScaleDB*/
  DUPLICATE_DETECTED, /*There were some records that were found to be duplicate in timeScaleDB*/
  STATUS_MISMATCH_DETECTED,
  DUPLICATE_DETECTED_MISSING_RECORDS_DETECTED, /*Some records were missing and some duplicates were found*/
  DUPLICATE_DETECTED_STATUS_MISMATCH_DETECTED,
  MISSING_RECORDS_DETECTED_STATUS_MISMATCH_DETECTED,
  DUPLICATE_DETECTED_MISSING_RECORDS_DETECTED_STATUS_MISMATCH_DETECTED,
  ERROR /*Detection failed*/
}
