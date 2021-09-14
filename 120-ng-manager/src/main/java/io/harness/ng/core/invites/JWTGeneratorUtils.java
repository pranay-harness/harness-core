/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.invites;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.JWTTokenServiceUtils;

import com.auth0.jwt.interfaces.Claim;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//  I want to have JWTGeneratorUtils as util class but how will I inject dependencies then?
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(PL)
public class JWTGeneratorUtils {
  public static final String HARNESS_USER = "harnessUser";
  // A claim in the token to denote which environment the token is generated.
  public static final String ENV = "env";

  private static final String ISSUER = "Harness Inc";

  public String generateJWTToken(Map<String, String> claims, Long validityDurationInMillis, String jwtPasswordSecret) {
    return JWTTokenServiceUtils.generateJWTToken(claims, validityDurationInMillis, jwtPasswordSecret);
  }

  public Map<String, Claim> verifyJWTToken(String token, String jwtPasswordSecret) {
    return JWTTokenServiceUtils.verifyJWTToken(token, jwtPasswordSecret);
  }
}
