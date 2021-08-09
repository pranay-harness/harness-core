package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;
import io.harness.security.dto.Principal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("PrincipalContextData")
public class PrincipalContextData implements GlobalContextData {
  public static final String PRINCIPAL_CONTEXT = "PRINCIPAL_CONTEXT";

  Principal principal;

  @Override
  public String getKey() {
    return PRINCIPAL_CONTEXT;
  }
}
