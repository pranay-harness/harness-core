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
    return chain.proceed(request.newBuilder().header("Authorization", "Bearer " + token).build());
  }
}
