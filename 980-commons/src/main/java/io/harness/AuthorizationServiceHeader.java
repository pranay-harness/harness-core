package io.harness;

public enum AuthorizationServiceHeader {
  BEARER("Bearer"),
  MANAGER("Manager"),
  NG_MANAGER("NextGenManager"),
  CI_MANAGER("CIManager"),
  CV_NEXT_GEN("CVNextGen"),
  IDENTITY_SERVICE("IdentityService"),
  ADMIN_PORTAL("AdminPortal"),
  NOTIFICATION_SERVICE("NotificationService"),
  DEFAULT("Default");

  private final String serviceId;

  AuthorizationServiceHeader(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getServiceId() {
    return serviceId;
  }
}
