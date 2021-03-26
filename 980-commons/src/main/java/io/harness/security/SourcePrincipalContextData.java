package io.harness.security;

import io.harness.context.GlobalContextData;
import io.harness.security.dto.Principal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourcePrincipalContextData implements GlobalContextData {
  public static final String SOURCE_PRINCIPAL = "SOURCE_PRINCIPAL";
  Principal principal;

  @Override
  public String getKey() {
    return SOURCE_PRINCIPAL;
  }
}
