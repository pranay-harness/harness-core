package io.harness.managerclient;

import static io.harness.network.Localhost.getLocalHostName;

import io.harness.security.TokenGenerator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class DelegateAuthInterceptor implements Interceptor {
  private static final String HOST_NAME = getLocalHostName();

  private TokenGenerator tokenGenerator;

  public DelegateAuthInterceptor(TokenGenerator tokenGenerator) {
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    String scheme = chain.request().url().scheme();
    String host = chain.request().url().host();
    int port = chain.request().url().port();

    String token = tokenGenerator.getToken(scheme, host, port, HOST_NAME);

    Request request = chain.request();
    return chain.proceed(request.newBuilder().header("Authorization", "Delegate " + token).build());
  }
}
