package io.harness.security;

import io.harness.context.GlobalContextData;
import io.harness.security.dto.Principal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrincipalContextData implements GlobalContextData {
  public static final String PRINCIPAL_CONTEXT = "PRINCIPAL_CONTEXT";

  Principal principal;

  @Override
  public String getKey() {
    return PRINCIPAL_CONTEXT;
  }
}
