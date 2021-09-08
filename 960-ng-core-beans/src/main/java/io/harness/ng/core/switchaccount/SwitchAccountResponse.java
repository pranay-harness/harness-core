package io.harness.ng.core.switchaccount;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SwitchAccountResponse {
  boolean requiresReAuthentication;
}
