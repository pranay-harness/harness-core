package io.harness.ng.apikeys.service.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.apikeys.dto.TokenDTO;

@OwnedBy(PL)
public interface ApiKeyService {
  String createToken(String accountIdentifier, TokenDTO tokenDTO);
}
