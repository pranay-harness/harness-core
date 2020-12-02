package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.auth0.jwt.interfaces.Claim;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(PL)
public interface JWTTokenHandler {
  Pair<Boolean, Map<String, Claim> > validate(String token, String secret);
}
