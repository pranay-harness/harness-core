/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.reconciliation.deployment;

public enum DetectionStatus {
  SUCCESS, /*No issues found*/
  MISSING_RECORDS_DETECTED, /*There were some records missing in the timeScaleDB*/
  DUPLICATE_DETECTED, /*There were some records that were found to be duplicate in timeScaleDB*/
  STATUS_MISMATCH_DETECTED, /*There were some mismatched records in mongoDB and timeScaleDB*/
  DUPLICATE_DETECTED_MISSING_RECORDS_DETECTED, /*Some records were missing and some duplicates were found*/
  DUPLICATE_DETECTED_STATUS_MISMATCH_DETECTED, /*Some records were mismatched and some duplicates were found*/
  MISSING_RECORDS_DETECTED_STATUS_MISMATCH_DETECTED, /*Some records were missing and some were mismatched*/
  DUPLICATE_DETECTED_MISSING_RECORDS_DETECTED_STATUS_MISMATCH_DETECTED, /*Some records were missing, some were
                                                                           mismatched and some duplicates were found*/
  ERROR /*Detection failed*/
}
