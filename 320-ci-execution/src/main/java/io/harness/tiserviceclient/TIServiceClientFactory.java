package io.harness.tiserviceclient;

import io.harness.ci.beans.entities.TIServiceConfig;
import io.harness.network.Http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TIServiceClientFactory implements Provider<TIServiceClient> {
  private TIServiceConfig tiConfig;

  @Inject
  public TIServiceClientFactory(TIServiceConfig tiConfig) {
    this.tiConfig = tiConfig;
  }

  @Override
  public TIServiceClient get() {
    Gson gson = new GsonBuilder().setLenient().create();
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(tiConfig.getBaseUrl())
                            .client(Http.getUnsafeOkHttpClient(tiConfig.getBaseUrl()))
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
    return retrofit.create(TIServiceClient.class);
  }
}
