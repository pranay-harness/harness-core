package io.harness.security.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import lombok.Getter;

@OwnedBy(PL)
@Getter
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
public abstract class Principal {
  private final PrincipalType type;
  private final String name;

  public Principal(PrincipalType type, String name) {
    this.type = type;
    this.name = name;
  }

  public abstract Map<String, String> getJWTClaims();
}
