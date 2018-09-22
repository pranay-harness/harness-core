package software.wings.managerclient;

import io.harness.security.TokenGenerator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import software.wings.delegate.service.DelegateServiceImpl;

import java.io.IOException;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
public class DelegateAuthInterceptor implements Interceptor {
  private TokenGenerator tokenGenerator;

  public DelegateAuthInterceptor(TokenGenerator tokenGenerator) {
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    String scheme = chain.request().url().scheme();
    String host = chain.request().url().host();
    int port = chain.request().url().port();

    String token = tokenGenerator.getToken(scheme, host, port, DelegateServiceImpl.getHostName());

    Request request = chain.request();
    return chain.proceed(request.newBuilder().header("Authorization", "Delegate " + token).build());
  }
}
