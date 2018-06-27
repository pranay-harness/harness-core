package software.wings.service.impl.dynatrace;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.network.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.DynaTraceConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.dynatrace.DynaTraceRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Created by rsingh on 1/29/18.
 */
public class DynaTraceDelegateServiceImpl implements DynaTraceDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(DynaTraceDelegateServiceImpl.class);
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;

  @Override
  public boolean validateConfig(DynaTraceConfig dynaTraceConfig) throws IOException {
    final Call<Object> request =
        getDynaTraceRestClient(dynaTraceConfig)
            .listTimeSeries(getHeaderWithCredentials(dynaTraceConfig, Collections.emptyList()));
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return true;
    } else {
      logger.error("Request not successful. Reason: {}", response);
      throw new WingsException(response.errorBody().string());
    }
  }

  @Override
  public DynaTraceMetricDataResponse fetchMetricData(DynaTraceConfig dynaTraceConfig,
      DynaTraceMetricDataRequest dataRequest, List<EncryptedDataDetail> encryptedDataDetails,
      ThirdPartyApiCallLog apiCallLog) throws IOException {
    Preconditions.checkNotNull(apiCallLog);
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
    apiCallLog.setRequest("url: " + dynaTraceConfig.getDynaTraceUrl() + " request: " + dataRequest);
    final Call<DynaTraceMetricDataResponse> request =
        getDynaTraceRestClient(dynaTraceConfig)
            .fetchMetricData(getHeaderWithCredentials(dynaTraceConfig, encryptedDataDetails), dataRequest);
    final Response<DynaTraceMetricDataResponse> response = request.execute();
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
    apiCallLog.setStatusCode(response.code());
    if (response.isSuccessful()) {
      apiCallLog.setJsonResponse(response.body());
      delegateLogService.save(dynaTraceConfig.getAccountId(), apiCallLog);
      return response.body();
    } else {
      apiCallLog.setJsonResponse(response.errorBody());
      delegateLogService.save(dynaTraceConfig.getAccountId(), apiCallLog);
      logger.error("Request not successful. Reason: {}, request: {}", response, dataRequest);
      throw new WingsException(response.errorBody().string());
    }
  }

  private DynaTraceRestClient getDynaTraceRestClient(final DynaTraceConfig dynaTraceConfig) {
    final Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(dynaTraceConfig.getDynaTraceUrl())
            .addConverterFactory(JacksonConverterFactory.create())
            .client(Http.getOkHttpClientWithNoProxyValueSet(dynaTraceConfig.getDynaTraceUrl()).build())
            .build();
    return retrofit.create(DynaTraceRestClient.class);
  }

  private String getHeaderWithCredentials(
      DynaTraceConfig dynaTraceConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(dynaTraceConfig, encryptionDetails);
    return "Api-Token " + new String(dynaTraceConfig.getApiToken());
  }
}
