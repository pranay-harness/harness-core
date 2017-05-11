package software.wings.service.impl.appdynamics;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.appdynamics.AppdynamicsRestClient;
import software.wings.service.intfc.appdynamics.AppdynamicsDeletegateService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by rsingh on 4/17/17.
 */
public class AppdynamicsDeletegateServiceImpl implements AppdynamicsDeletegateService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public List<AppdynamicsApplicationResponse> getAllApplications(AppDynamicsConfig appDynamicsConfig)
      throws IOException {
    final Call<List<AppdynamicsApplicationResponse>> request =
        getAppdynamicsRestClient(appDynamicsConfig).listAllApplications(getHeaderWithCredentials(appDynamicsConfig));
    final Response<List<AppdynamicsApplicationResponse>> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    } else {
      logger.error("Request not successful. Reason: {}", response);
      throw new WingsException("could not get appdynamics applications");
    }
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
