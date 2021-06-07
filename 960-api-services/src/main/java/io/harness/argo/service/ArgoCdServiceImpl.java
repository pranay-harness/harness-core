package io.harness.argo.service;

import com.google.gson.JsonObject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.argo.ArgoRestClient;
import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.argo.beans.ArgoToken;
import io.harness.argo.beans.UsernamePassword;
import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;

@OwnedBy(HarnessTeam.CDP)
@Singleton
@Slf4j
public class ArgoCdServiceImpl implements ArgoCdService {
  public static final String BEARER = "Bearer ";

  @Override
  public String fetchApplication(ArgoConfigInternal argoConfig, String argoAppName) throws IOException {
    String app = null;
    ArgoRestClient argoRestClient = createArgoRestClient(argoConfig);
    String token = fetchToken(argoConfig, argoRestClient);
    Response<JsonObject> response = argoRestClient.fetchApp(BEARER + token, argoAppName).execute();

    app = token;
    return app;
  }

  private String fetchToken(ArgoConfigInternal argoConfig, ArgoRestClient argoRestClient) throws IOException {
    Response<ArgoToken> argoCdToken = argoRestClient
                                          .fetchToken(UsernamePassword.builder()
                                                          .username(argoConfig.getUsername())
                                                          .password(argoConfig.getPassword())
                                                          .build())
                                          .execute();
    if (argoCdToken.isSuccessful()) {
      //
    }

    return argoCdToken.body().getToken();

    //    return argoCdToken.getToken();
  }

  private ArgoRestClient createArgoRestClient(ArgoConfigInternal argoConfig) {
    OkHttpClient okHttpClient =
        Http.getOkHttpClient(argoConfig.getArgoServerUrl(), argoConfig.isCertValidationRequired());
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(argoConfig.getArgoServerUrl())
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(ArgoRestClient.class);
  }
}
