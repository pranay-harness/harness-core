package io.harness.security;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
public class VerificationAuthInterceptor implements Interceptor {
  private VerificationTokenGenerator tokenGenerator;

  public VerificationAuthInterceptor(VerificationTokenGenerator tokenGenerator) {
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    String token = tokenGenerator.getToken();

    Request request = chain.request();
    return chain.proceed(request.newBuilder().header("Authorization", "LearningEngine " + token).build());
  }
}
