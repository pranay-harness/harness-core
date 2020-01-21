package software.wings.service.impl.instana;

import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.getUnsafeHttpClient;

import com.google.inject.Inject;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.InstanaConfig;
import software.wings.delegatetasks.cv.RequestExecutor;
import software.wings.helpers.ext.instana.InstanaRestClient;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.instana.InstanaDelegateService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
@Slf4j
public class InstanaDelegateServiceImpl implements InstanaDelegateService {
  @Inject private EncryptionService encryptionService;
  @Inject private RequestExecutor requestExecutor;

  @Override
  public InstanaInfraMetrics getInfraMetrics(InstanaConfig instanaConfig, List<EncryptedDataDetail> encryptionDetails,
      InstanaInfraMetricRequest infraMetricRequest, ThirdPartyApiCallLog apiCallLog) {
    apiCallLog.setTitle("Fetching Infrastructure metrics from " + instanaConfig.getInstanaUrl());
    final Call<InstanaInfraMetrics> request =
        getRestClient(instanaConfig)
            .getInfrastructureMetrics(getAuthorizationHeader(instanaConfig, encryptionDetails), infraMetricRequest);
    return requestExecutor.executeRequest(apiCallLog, request);
  }

  InstanaRestClient getRestClient(final InstanaConfig instanaConfig) {
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(instanaConfig.getInstanaUrl())
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(getUnsafeHttpClient(instanaConfig.getInstanaUrl()))
                                  .build();
    return retrofit.create(InstanaRestClient.class);
  }

  private String getAuthorizationHeader(InstanaConfig instanaConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(instanaConfig, encryptionDetails);
    return "apiToken " + new String(instanaConfig.getApiToken());
  }
}
