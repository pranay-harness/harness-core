package io.harness.security;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

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
