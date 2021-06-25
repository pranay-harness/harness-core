package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.TokenDTO;

import java.time.Instant;

@OwnedBy(PL)
public interface TokenService {
  String createToken(TokenDTO tokenDTO);
  boolean revokeToken(String tokenIdentifier);
  String rotateToken(String tokenIdentifier, Instant scheduledExpireTime);
  TokenDTO updateToken(TokenDTO tokenDTO);
}
