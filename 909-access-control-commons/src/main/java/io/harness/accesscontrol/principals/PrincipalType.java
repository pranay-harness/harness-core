package io.harness.accesscontrol.principals;

public enum PrincipalType {
  USER,
  USER_GROUP,
  API_KEY,
  SERVICE;

  public static PrincipalType fromPrincipalTypeOfContext(io.harness.security.dto.PrincipalType principalType) {
    switch (principalType) {
      case SERVICE:
        return SERVICE;
      case API_KEY:
        return API_KEY;
      case USER:
        return USER;
      default:
        return null;
    }
  }
}
