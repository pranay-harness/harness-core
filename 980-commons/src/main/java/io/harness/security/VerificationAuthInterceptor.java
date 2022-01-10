/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.security;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
public class VerificationAuthInterceptor implements Interceptor {
  private ServiceTokenGenerator tokenGenerator;

  public VerificationAuthInterceptor(ServiceTokenGenerator tokenGenerator) {
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    String token = tokenGenerator.getVerificationServiceToken();

    Request request = chain.request();
    return chain.proceed(request.newBuilder().header("Authorization", "LearningEngine " + token).build());
  }
}
