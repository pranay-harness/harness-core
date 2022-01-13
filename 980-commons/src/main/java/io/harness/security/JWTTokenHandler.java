/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
