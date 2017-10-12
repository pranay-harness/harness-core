package software.wings.service.impl.appdynamics;

import static software.wings.utils.HttpUtil.validUrl;

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
import software.wings.service.impl.appdynamics.AppdynamicsMetric.AppdynamicsMetricType;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by rsingh on 4/17/17.
 */
public class AppdynamicsDelegateServiceImpl implements AppdynamicsDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(AppdynamicsDelegateServiceImpl.class);
  private static final String BT_PERFORMANCE_PATH_PREFIX = "Business Transaction Performance|Business Transactions|";

  @Override
  public List<NewRelicApplication> getAllApplications(AppDynamicsConfig appDynamicsConfig) throws IOException {
    final Call<List<NewRelicApplication>> request =
        getAppdynamicsRestClient(appDynamicsConfig).listAllApplications(getHeaderWithCredentials(appDynamicsConfig));
    final Response<List<NewRelicApplication>> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    } else {
      logger.error("Request not successful. Reason: {}", response);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, "reason", "could not fetch Appdynamics applications");
    }
  }

  @Override
  public List<AppdynamicsTier> getTiers(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId) throws IOException {
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
  public List<AppdynamicsNode> getNodes(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId)
      throws IOException {
    final Call<List<AppdynamicsNode>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listNodes(getHeaderWithCredentials(appDynamicsConfig), appdynamicsAppId, tierId);
    final Response<List<AppdynamicsNode>> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    } else {
      logger.error("Request not successful. Reason: {}", response);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, "reason", "could not fetch Appdynamics nodes");
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
  public List<AppdynamicsMetric> getTierBTMetrics(
      AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId) throws IOException {
    final AppdynamicsTier tier = getAppdynamicsTier(appDynamicsConfig, appdynamicsAppId, tierId);
    final String tierBTsPath = BT_PERFORMANCE_PATH_PREFIX + tier.getName();
    Call<List<AppdynamicsMetric>> tierBTMetricRequest =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listMetrices(getHeaderWithCredentials(appDynamicsConfig), appdynamicsAppId, tierBTsPath);

    final Response<List<AppdynamicsMetric>> tierBTResponse = tierBTMetricRequest.execute();
    if (!tierBTResponse.isSuccessful()) {
      logger.error("Request not successful. Reason: {}", tierBTResponse);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, "reason", "could not fetch Appdynamics tier BTs");
    }

    List<AppdynamicsMetric> rv = tierBTResponse.body();
    for (AppdynamicsMetric appdynamicsTierMetric : rv) {
      appdynamicsTierMetric.setChildMetrices(
          getChildMetrics(appDynamicsConfig, appdynamicsAppId, appdynamicsTierMetric, tierBTsPath + "|"));
    }

    return rv;
  }

  @Override
  public List<AppdynamicsMetricData> getTierBTMetricData(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      long tierId, String btName, int durantionInMinutes) throws IOException {
    logger.debug("getting AppDynamics metric data");
    final AppdynamicsTier tier = getAppdynamicsTier(appDynamicsConfig, appdynamicsAppId, tierId);
    String metricPath = BT_PERFORMANCE_PATH_PREFIX + tier.getName() + "|" + btName + "|"
        + "Individual Nodes|*|*";
    Call<List<AppdynamicsMetricData>> tierBTMetricRequest =
        getAppdynamicsRestClient(appDynamicsConfig)
            .getMetricData(
                getHeaderWithCredentials(appDynamicsConfig), appdynamicsAppId, metricPath, durantionInMinutes);

    final Response<List<AppdynamicsMetricData>> tierBTMResponse = tierBTMetricRequest.execute();
    if (tierBTMResponse.isSuccessful()) {
      logger.debug("AppDynamics metric data found: " + tierBTMResponse.body().size() + " records.");
      return tierBTMResponse.body();
    } else {
      logger.error("Request not successful. Reason: {}", tierBTMResponse);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, "reason", "could not fetch Appdynamics metric data");
    }
  }

  private AppdynamicsTier getAppdynamicsTier(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId)
      throws IOException {
    final Call<List<AppdynamicsTier>> tierDetail =
        getAppdynamicsRestClient(appDynamicsConfig)
            .getTierDetails(getHeaderWithCredentials(appDynamicsConfig), appdynamicsAppId, tierId);
    final Response<List<AppdynamicsTier>> tierResponse = tierDetail.execute();
    if (!tierResponse.isSuccessful()) {
      logger.error("Request not successful. Reason: {}", tierResponse);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, "reason", "could not fetch Appdynamics tier details");
    }

    return tierResponse.body().get(0);
  }

  private List<AppdynamicsMetric> getChildMetrics(AppDynamicsConfig appDynamicsConfig, long applicationId,
      AppdynamicsMetric appdynamicsMetric, String parentMetricPath) throws IOException {
    if (appdynamicsMetric.getType() != AppdynamicsMetricType.folder) {
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

        // While getting the metric names we do not need to go to individual metrics names since the metric names in
        // each node are the same and there can be thousands of nodes in case of recycled nodes for container world
        // We would not be monitoring external calls metrics because one deployment is not going to effect multiple
        // tiers
        if (metric.getName().contains("Individual Nodes") || metric.getName().contains("External Calls")) {
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

  @Override
  public void validateConfig(AppDynamicsConfig appDynamicsConfig) throws IOException {
    if (!validUrl(appDynamicsConfig.getControllerUrl())) {
      throw new RuntimeException("AppDynamics Controller URL must be a valid URL");
    }
    Response<List<NewRelicApplication>> response = null;
    try {
      final Call<List<NewRelicApplication>> request =
          getAppdynamicsRestClient(appDynamicsConfig).listAllApplications(getHeaderWithCredentials(appDynamicsConfig));
      response = request.execute();
      if (response.isSuccessful()) {
        return;
      }
    } catch (Throwable t) {
      throw new RuntimeException("Could not reach AppDynamics server. " + t.getMessage());
    }

    final int errorCode = response.code();
    if (errorCode == HttpStatus.SC_UNAUTHORIZED) {
      throw new RuntimeException("Could not login to AppDynamics server with the given credentials");
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
        + Base64.encodeBase64String(
              String
                  .format("%s@%s:%s", appDynamicsConfig.getUsername(), appDynamicsConfig.getAccountname(),
                      new String(appDynamicsConfig.getPassword()))
                  .getBytes(StandardCharsets.UTF_8));
  }
}
