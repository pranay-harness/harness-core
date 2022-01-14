package io.harness.globalcontex;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;

import lombok.Builder;
import lombok.Value;

@OwnedBy(DEL)
@Value
@Builder
public class DelegateTokenGlobalContextData implements GlobalContextData {
  public static final String TOKEN_NAME = "TOKEN_NAME";
  private String tokenName;

  @Override
  public String getKey() {
    return TOKEN_NAME;
  }
}
