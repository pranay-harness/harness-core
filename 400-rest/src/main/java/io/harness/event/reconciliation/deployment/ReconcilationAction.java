/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.reconciliation.deployment;

public enum ReconcilationAction {
  NONE,
  ADD_MISSING_RECORDS,
  DUPLICATE_REMOVAL,
  STATUS_RECONCILIATION,
  DUPLICATE_REMOVAL_ADD_MISSING_RECORDS,
  DUPLICATE_REMOVAL_STATUS_RECONCILIATION,
  ADD_MISSING_RECORDS_STATUS_RECONCILIATION,
  DUPLICATE_REMOVAL_ADD_MISSING_RECORDS_STATUS_RECONCILIATION
}
