package io.harness.security.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.security.SecurityContextBuilder.ACCOUNT_ID;
import static io.harness.security.SecurityContextBuilder.PRINCIPAL_NAME;
import static io.harness.security.SecurityContextBuilder.PRINCIPAL_TYPE;
import static io.harness.security.dto.PrincipalType.USER;

import io.harness.annotations.dev.OwnedBy;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@OwnedBy(PL)
@Getter
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("USER")
public class UserPrincipal extends Principal {
  String accountId;

  public UserPrincipal(String principal, String accountId) {
    super(USER, principal);
    this.accountId = accountId;
  }

  @Override
  public Map<String, String> getJWTClaims() {
    Map<String, String> claims = new HashMap<>();
    claims.put(PRINCIPAL_TYPE, getType().toString());
    claims.put(PRINCIPAL_NAME, getName());
    claims.put(ACCOUNT_ID, getAccountId());
    return claims;
  }

  public static UserPrincipal getPrincipal(Map<String, Claim> claims) {
    return new UserPrincipal(claims.get(PRINCIPAL_NAME) == null ? null : claims.get(PRINCIPAL_NAME).asString(),
        claims.get(ACCOUNT_ID) == null ? null : claims.get(ACCOUNT_ID).asString());
  }
}
