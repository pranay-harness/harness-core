package io.harness.ng.core.switchaccount;

import io.harness.ng.core.account.OauthProviderType;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class OauthIdentificationInfo {
  Set<OauthProviderType> providers;
}
