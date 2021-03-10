package io.harness.managerclient;

import static io.harness.network.Localhost.getLocalHostName;

import io.harness.security.TokenGenerator;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class WatcherAuthInterceptor implements Interceptor {
  private TokenGenerator tokenGenerator;

  WatcherAuthInterceptor(TokenGenerator tokenGenerator) {
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    String scheme = chain.request().url().scheme();
    String host = chain.request().url().host();
    int port = chain.request().url().port();

    String token = tokenGenerator.getToken(scheme, host, port, getLocalHostName());

    Request request = chain.request();
    return chain.proceed(request.newBuilder().header("Authorization", "Delegate " + token).build());
  }
}
