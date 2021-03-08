package io.harness.security.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Getter
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@FieldNameConstants(innerTypeName = "PrincipalKeys")
public abstract class Principal {
  @NotNull private final PrincipalType type;
  @NotBlank private final String name;

  public Principal(PrincipalType type, String name) {
    this.type = type;
    this.name = name;
  }

  public abstract Map<String, String> getJWTClaims();
}
