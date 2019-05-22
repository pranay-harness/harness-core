package io.harness.event.model;

public enum EventType {
  USER_INVITED_FROM_EXISTING_ACCOUNT,
  COMPLETE_USER_REGISTRATION,
  FIRST_DELEGATE_REGISTERED,
  FIRST_WORKFLOW_CREATED,
  FIRST_DEPLOYMENT_EXECUTED,
  FIRST_VERIFIED_DEPLOYMENT,
  FIRST_ROLLED_BACK_DEPLOYMENT,
  SETUP_CV_24X7,
  SETUP_2FA,
  SETUP_SSO,
  SETUP_IP_WHITELISTING,
  SETUP_RBAC,
  TRIAL_TO_PAID,
  TRIAL_TO_COMMUNITY,
  COMMUNITY_TO_PAID,

  /***
   * Usage metrics EventTypes
   */
  DEPLOYMENT_METADATA,
  DEPLOYMENT_DURATION,
  USERS_LOGGED_IN,
  SETUP_DATA,
  INSTANCE_COUNT,
  LICENSE_UNITS,

  OPEN_ALERT,

  NEW_TRIAL_SIGNUP,
  LICENSE_UPDATE,

  DEPLOYMENT_VERIFIED,
  JOIN_ACCOUNT_REQUEST
}
