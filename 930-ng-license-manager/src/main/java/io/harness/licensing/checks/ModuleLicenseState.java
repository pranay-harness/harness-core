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
