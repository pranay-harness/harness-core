/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.licensing.checks;

public enum ModuleLicenseState {
  NO_LICENSE,
  ACTIVE_FREE,
  ACTIVE_TEAM_TRIAL,
  EXPIRED_TEAM_TRIAL_CAN_EXTEND,
  EXPIRED_TEAM_TRIAL,
  ACTIVE_TEAM_PAID,
  EXPIRED_TEAM_PAID,
  ACTIVE_ENTERPRISE_TRIAL,
  EXPIRED_ENTERPRISE_TRIAL_CAN_EXTEND,
  EXPIRED_ENTERPRISE_TRIAL,
  ACTIVE_ENTERPRISE_PAID,
  EXPIRED_ENTERPRISE_PAID
}
