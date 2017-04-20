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
import software.wings.service.impl.appdynamics.AppdynamicsMetric.AppdynamicsMetricType;
import software.wings.service.intfc.appdynamics.AppdynamicsDeletegateService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
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

  @Override
  public List<AppdynamicsMetric> getAllMetrics(AppDynamicsConfig appDynamicsConfig, final int applicationId)
      throws IOException {
    final Call<List<AppdynamicsMetric>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listParentMetrics(getHeaderWithCredentials(appDynamicsConfig), applicationId);
    final Response<List<AppdynamicsMetric>> response = request.execute();
    if (response.isSuccessful()) {
      final List<AppdynamicsMetric> allMetrices = response.body();
      for (Iterator<AppdynamicsMetric> iterator = allMetrices.iterator(); iterator.hasNext();) {
        AppdynamicsMetric appdynamicsMetric = iterator.next();
        if (appdynamicsMetric.getName().contains("Application Infrastructure Performance")) {
          iterator.remove();
          continue;
        }
        appdynamicsMetric.setChildMetrices(getChildMetrics(appDynamicsConfig, applicationId, appdynamicsMetric, ""));
      }
      return allMetrices;
    } else {
      logger.error("Request not successful. Reason: {}", response);
      throw new WingsException("could not get appdynami's metrics");
    }
  }

  private List<AppdynamicsMetric> getChildMetrics(AppDynamicsConfig appDynamicsConfig, int applicationId,
      AppdynamicsMetric appdynamicsMetric, String parentMetricPath) throws IOException {
    if (appdynamicsMetric.getType() != AppdynamicsMetricType.folder) {
      return Collections.emptyList();
    }

    if (parentMetricPath.contains("Overall Application Performance")) {
      return Collections.emptyList();
    }

    final String childMetricPath = parentMetricPath + appdynamicsMetric.getName() + "|";
    Call<List<AppdynamicsMetric>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listMetrices(getHeaderWithCredentials(appDynamicsConfig), applicationId, childMetricPath);
    final Response<List<AppdynamicsMetric>> response = request.execute();
    if (response.isSuccessful()) {
      final List<AppdynamicsMetric> allMetrices = response.body();
      for (Iterator<AppdynamicsMetric> iterator = allMetrices.iterator(); iterator.hasNext();) {
        final AppdynamicsMetric metric = iterator.next();
        if (metric.getName().contains("Individual Nodes")) {
          iterator.remove();
          continue;
        }

        metric.setChildMetrices(getChildMetrics(appDynamicsConfig, applicationId, metric, childMetricPath));
      }
      return allMetrices;
    } else {
      logger.error("Request not successful. Reason: {}", response);
      throw new WingsException("could not get appdynami's metrics");
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
