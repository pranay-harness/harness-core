/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.security;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.io.IOException;
import lombok.AllArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

@OwnedBy(HarnessTeam.PIPELINE)
@AllArgsConstructor
public class PmsAuthInterceptor implements Interceptor {
  private final String jwtAuthSecret;

  @Override
  public Response intercept(Chain chain) throws IOException {
    ServiceTokenGenerator tokenGenerator = ServiceTokenGenerator.newInstance();
    String token = tokenGenerator.getServiceToken(jwtAuthSecret);

    Request request = chain.request();
    return chain.proceed(request.newBuilder().header("Authorization", "ApiKey " + token).build());
  }
}
