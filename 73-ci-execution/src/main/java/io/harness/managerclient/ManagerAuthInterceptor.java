package io.harness.managerclient;

import io.harness.security.ServiceTokenGenerator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Add token on request for authentication,
 * temporarily we are using verification token then we will migrate to delegate microservice
 */

public class ManagerAuthInterceptor implements Interceptor {
  private ServiceTokenGenerator tokenGenerator;

  public ManagerAuthInterceptor(ServiceTokenGenerator tokenGenerator) {
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    String token = tokenGenerator.getVerificationServiceToken();
    Request request = chain.request();
    /* TODO
     Write CI authentication and token builder, Temporarily we are using learning engine token
     Timeout is high because of sync tasks,
     Temporary code till we don't have delegate microservice ready
     **
     */

    return chain.withWriteTimeout(50, TimeUnit.MINUTES)
        .withReadTimeout(50, TimeUnit.MINUTES)
        .withConnectTimeout(50, TimeUnit.MINUTES)
        .proceed(request.newBuilder().header("Authorization", "LearningEngine " + token).build());
  }
}
