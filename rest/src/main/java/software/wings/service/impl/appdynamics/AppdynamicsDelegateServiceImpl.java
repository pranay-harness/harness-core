package software.wings.service.impl.appdynamics;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.appdynamics.AppdynamicsRestClient;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by rsingh on 4/17/17.
 */
public class AppdynamicsDelegateServiceImpl implements AppdynamicsDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(AppdynamicsDelegateServiceImpl.class);

  @Override
  public List<AppdynamicsApplication> getAllApplications(AppDynamicsConfig appDynamicsConfig) throws IOException {
    final Call<List<AppdynamicsApplication>> request =
        getAppdynamicsRestClient(appDynamicsConfig).listAllApplications(getHeaderWithCredentials(appDynamicsConfig));
    final Response<List<AppdynamicsApplication>> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    } else {
      logger.error("Request not successful. Reason: {}", response);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, "reason", "could not fetch Appdynamics applications");
    }
  }

  @Override
  public List<AppdynamicsTier> getTiers(AppDynamicsConfig appDynamicsConfig, int appdynamicsAppId) throws IOException {
    final Call<List<AppdynamicsTier>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listTiers(getHeaderWithCredentials(appDynamicsConfig), appdynamicsAppId);
    final Response<List<AppdynamicsTier>> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    } else {
      logger.error("Request not successful. Reason: {}", response);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, "reason", "could not fetch Appdynamics tiers");
    }
  }

  @Override
  public List<AppdynamicsBusinessTransaction> getBusinessTransactions(
      AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId) throws IOException {
    final Call<List<AppdynamicsBusinessTransaction>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listBusinessTransactions(getHeaderWithCredentials(appDynamicsConfig), appdynamicsAppId);
    final Response<List<AppdynamicsBusinessTransaction>> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    } else {
      logger.error("Request not successful. Reason: {}", response);
      throw new WingsException(
          ErrorCode.APPDYNAMICS_ERROR, "reason", "could not fetch Appdynamics business transactions");
    }
  }

  @Override
  public void validateConfig(AppDynamicsConfig appDynamicsConfig) throws IOException {
    Response<List<AppdynamicsApplication>> response = null;
    try {
      final Call<List<AppdynamicsApplication>> request =
          getAppdynamicsRestClient(appDynamicsConfig).listAllApplications(getHeaderWithCredentials(appDynamicsConfig));
      response = request.execute();
      if (response.isSuccessful()) {
        return;
      }
    } catch (Throwable t) {
      throw new RuntimeException("Could not reach Appdynamics server. " + t.getMessage());
    }

    final int errorCode = response.code();
    if (errorCode == HttpStatus.SC_UNAUTHORIZED) {
      throw new RuntimeException("Could not login to Appdynamics server with given credentials");
    }

    throw new RuntimeException(response.message());
  }

  private AppdynamicsRestClient getAppdynamicsRestClient(final AppDynamicsConfig appDynamicsConfig) {
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(appDynamicsConfig.getControllerUrl() + "/")
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .build();
    return retrofit.create(AppdynamicsRestClient.class);
  }

  private String getHeaderWithCredentials(AppDynamicsConfig appDynamicsConfig) {
    return "Basic "
        + Base64.encodeBase64String(String
                                        .format("%s@%s:%s", appDynamicsConfig.getUsername(),
                                            appDynamicsConfig.getAccountname(), appDynamicsConfig.getPassword())
                                        .getBytes(StandardCharsets.UTF_8));
  }
}
