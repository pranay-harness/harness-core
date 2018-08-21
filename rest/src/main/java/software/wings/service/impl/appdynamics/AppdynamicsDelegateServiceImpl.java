package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.Http.validUrl;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.network.Http;
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
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.appdynamics.AppdynamicsRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.appdynamics.AppdynamicsMetric.AppdynamicsMetricType;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by rsingh on 4/17/17.
 */
public class AppdynamicsDelegateServiceImpl implements AppdynamicsDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(AppdynamicsDelegateServiceImpl.class);
  public static final String BT_PERFORMANCE_PATH_PREFIX = "Business Transaction Performance|Business Transactions|";
  public static final String EXTERNAL_CALLS = "External Calls";
  public static final String INDIVIDUAL_NODES = "Individual Nodes";
  @Inject private EncryptionService encryptionService;

  @Inject private DataCollectionExecutorService dataCollectionService;
  @Inject private DelegateLogService delegateLogService;

  @Override
  public List<NewRelicApplication> getAllApplications(
      AppDynamicsConfig appDynamicsConfig, List<EncryptedDataDetail> encryptionDetails) throws IOException {
    final Call<List<NewRelicApplication>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listAllApplications(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails));
    final Response<List<NewRelicApplication>> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    } else {
      logger.info("Request not successful. Reason: {}", response);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
          .addParam("reason", "could not fetch Appdynamics applications");
    }
  }

  @Override
  public Set<AppdynamicsTier> getTiers(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      List<EncryptedDataDetail> encryptionDetails) throws IOException {
    final Call<Set<AppdynamicsTier>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listTiers(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId);
    final Response<Set<AppdynamicsTier>> response = request.execute();
    if (response.isSuccessful()) {
      response.body().forEach(tier -> tier.setExternalTiers(new HashSet<>()));
      return response.body();
    } else {
      logger.info("Request not successful. Reason: {}", response);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR).addParam("reason", "could not fetch Appdynamics tiers");
    }
  }

  @Override
  public Set<AppdynamicsTier> getTierDependencies(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      List<EncryptedDataDetail> encryptionDetails) throws IOException {
    Set<AppdynamicsTier> tiers = getTiers(appDynamicsConfig, appdynamicsAppId, encryptionDetails);
    List<Callable<Void>> callables = new ArrayList<>();
    tiers.forEach(tier -> callables.add(() -> {
      final String tierBTsPath = BT_PERFORMANCE_PATH_PREFIX + tier.getName();
      Call<List<AppdynamicsMetric>> tierBTMetricRequest =
          getAppdynamicsRestClient(appDynamicsConfig)
              .listMetrices(
                  getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId, tierBTsPath);

      try {
        final Response<List<AppdynamicsMetric>> tierBTResponse = tierBTMetricRequest.execute();
        if (!tierBTResponse.isSuccessful()) {
          logger.info("Request not successful. Reason: {}", tierBTResponse);
          throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
              .addParam("reason", "could not fetch Appdynamics tier BTs : " + tierBTResponse);
        }

        List<AppdynamicsMetric> tierBtMetrics = tierBTResponse.body();
        tierBtMetrics.forEach(tierBtMetric -> {
          try {
            List<AppdynamicsMetric> externalCallMetrics =
                getExternalCallMetrics(appDynamicsConfig, appdynamicsAppId, tierBtMetric, tierBTsPath + "|",
                    encryptionDetails, apiCallLogWithDummyStateExecution(appDynamicsConfig.getAccountId()));
            externalCallMetrics.forEach(
                externalCallMetric -> parseAndAddExternalTier(tier.getExternalTiers(), externalCallMetric, tiers));
          } catch (IOException e) {
            throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, e)
                .addParam("reason", "could not fetch Appdynamics tier BTs");
          } catch (CloneNotSupportedException e) {
            throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, e)
                .addParam("reason", "could not fetch Appdynamics tier BTs");
          }
        });
        return null;
      } catch (Exception e) {
        throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, e)
            .addParam("reason", "could not fetch Appdynamics tier BTs");
      }
    }));
    dataCollectionService.executeParrallel(callables);

    callables.clear();
    tiers.forEach(tier -> callables.add(() -> {
      tier.getExternalTiers().forEach(externalTier -> {
        tiers.forEach(parentTier -> {
          if (parentTier.equals(externalTier)) {
            externalTier.getExternalTiers().addAll(parentTier.getExternalTiers());
          }
        });
      });
      return null;
    }));
    dataCollectionService.executeParrallel(callables);
    return tiers;
  }

  private void parseAndAddExternalTier(
      Set<AppdynamicsTier> externalTiers, AppdynamicsMetric externalCallMetric, Set<AppdynamicsTier> allTiers) {
    List<AppdynamicsMetric> childMetrices = externalCallMetric.getChildMetrices();
    if (isEmpty(childMetrices)) {
      return;
    }

    childMetrices.forEach(childMetric -> {
      if (childMetric.getType() == AppdynamicsMetricType.folder) {
        for (AppdynamicsTier appdynamicsTier : allTiers) {
          if (childMetric.getName().equals(appdynamicsTier.getName())) {
            AppdynamicsTier externalTier = AppdynamicsTier.builder()
                                               .id(appdynamicsTier.getId())
                                               .name(appdynamicsTier.getName())
                                               .agentType(appdynamicsTier.getAgentType())
                                               .description(appdynamicsTier.getDescription())
                                               .build();
            externalTiers.add(externalTier);
            parseAndAddExternalTier(externalTier.getExternalTiers(), childMetric, allTiers);
          }
        }

        parseAndAddExternalTier(externalTiers, childMetric, allTiers);
      }
    });
  }

  @Override
  public List<AppdynamicsMetric> getTierBTMetrics(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      long tierId, List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog)
      throws IOException, CloneNotSupportedException {
    Preconditions.checkNotNull(apiCallLog);
    final AppdynamicsTier tier = getAppdynamicsTier(appDynamicsConfig, appdynamicsAppId, tierId, encryptionDetails);
    final String tierBTsPath = BT_PERFORMANCE_PATH_PREFIX + tier.getName();
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("url")
                                     .value(appDynamicsConfig.getControllerUrl() + "/rest/applications/"
                                         + appdynamicsAppId + "/metrics?output=JSON&metric-path=" + tierBTsPath)
                                     .type(FieldType.URL)
                                     .build());
    apiCallLog.setTitle("Fetching business transactions for tier from " + appDynamicsConfig.getControllerUrl());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    Call<List<AppdynamicsMetric>> tierBTMetricRequest =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listMetrices(
                getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId, tierBTsPath);

    final Response<List<AppdynamicsMetric>> tierBTResponse = tierBTMetricRequest.execute();
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    if (!tierBTResponse.isSuccessful()) {
      apiCallLog.addFieldToResponse(tierBTResponse.code(), tierBTResponse.errorBody().string(), FieldType.TEXT);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      logger.info("Request not successful. Reason: {}", tierBTResponse);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
          .addParam("reason",
              "Unsuccessful response while fetching tier BTs from AppDynamics. Error code: " + tierBTResponse.code()
                  + ". Error:" + tierBTResponse.errorBody());
    }

    apiCallLog.addFieldToResponse(tierBTResponse.code(), tierBTResponse.body(), FieldType.JSON);
    delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
    List<AppdynamicsMetric> rv = tierBTResponse.body();
    logger.info("metrics to analyze: " + rv);
    return rv;
  }

  @Override
  public List<AppdynamicsMetricData> getTierBTMetricData(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      String tierName, String btName, String hostName, int durantionInMinutes,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) throws IOException {
    Preconditions.checkNotNull(apiCallLog);
    logger.info("getting AppDynamics metric data for host {}, app {}, tier {}, bt {}", hostName, appdynamicsAppId,
        tierName, btName);
    String metricPath = BT_PERFORMANCE_PATH_PREFIX + tierName + "|" + btName + "|";

    metricPath += isEmpty(hostName) ? "*" : "Individual Nodes|" + hostName + "|*";
    logger.info("fetching metrics for path {} ", metricPath);
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder()
            .name("url")
            .value(appDynamicsConfig.getControllerUrl() + "/rest/applications/" + appdynamicsAppId
                + "/metric-data?output=JSON&time-range-type=BEFORE_NOW&rollup=false&duration-in-mins="
                + durantionInMinutes + "&metric-path=" + metricPath)
            .type(FieldType.URL)
            .build());
    apiCallLog.setTitle("Fetching metric data for " + metricPath);
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    Call<List<AppdynamicsMetricData>> tierBTMetricRequest =
        getAppdynamicsRestClient(appDynamicsConfig)
            .getMetricData(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId, metricPath,
                durantionInMinutes);

    final Response<List<AppdynamicsMetricData>> tierBTMResponse = tierBTMetricRequest.execute();
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    if (tierBTMResponse.isSuccessful()) {
      apiCallLog.addFieldToResponse(tierBTMResponse.code(), tierBTMResponse.body(), FieldType.JSON);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      if (logger.isDebugEnabled()) {
        logger.debug("AppDynamics metric data found: " + tierBTMResponse.body().size() + " records.");
      }
      logger.info("got {} metrics for path {}", tierBTMResponse.body().size(), metricPath);
      return tierBTMResponse.body();
    } else {
      logger.info("Request not successful. Reason: {}", tierBTMResponse);
      apiCallLog.addFieldToResponse(tierBTMResponse.code(), tierBTMResponse.errorBody().string(), FieldType.TEXT);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
          .addParam("reason",
              "Unsuccessful response while fetching data from AppDynamics. Error code: " + tierBTMResponse.code()
                  + ". Error message: " + tierBTMResponse.errorBody());
    }
  }

  public AppdynamicsTier getAppdynamicsTier(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId,
      List<EncryptedDataDetail> encryptionDetails) throws IOException {
    final Call<List<AppdynamicsTier>> tierDetail =
        getAppdynamicsRestClient(appDynamicsConfig)
            .getTierDetails(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId, tierId);
    final Response<List<AppdynamicsTier>> tierResponse = tierDetail.execute();
    if (!tierResponse.isSuccessful()) {
      logger.info("Request not successful. Reason: {}", tierResponse);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
          .addParam("reason",
              "Unsuccessful response while fetching data from AppDynamics. Error code: " + tierResponse.code()
                  + ". Error message: " + tierResponse.errorBody());
    }

    return tierResponse.body().get(0);
  }

  private List<AppdynamicsMetric> getChildMetrics(AppDynamicsConfig appDynamicsConfig, long applicationId,
      AppdynamicsMetric appdynamicsMetric, String parentMetricPath, boolean includeExternal,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog)
      throws IOException, CloneNotSupportedException {
    Preconditions.checkNotNull(apiCallLog);
    if (appdynamicsMetric.getType() != AppdynamicsMetricType.folder) {
      return Collections.emptyList();
    }

    if (parentMetricPath.contains("|" + appdynamicsMetric.getName() + "|")) {
      return Collections.emptyList();
    }

    final String childMetricPath = parentMetricPath + appdynamicsMetric.getName() + "|";
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("url")
                                     .value(appDynamicsConfig.getControllerUrl() + "/rest/applications/" + applicationId
                                         + "/metrics?output=JSON&metric-path=" + childMetricPath)
                                     .type(FieldType.URL)
                                     .build());
    apiCallLog.setTitle("Fetching metric names from " + appDynamicsConfig.getControllerUrl());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    Call<List<AppdynamicsMetric>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listMetrices(
                getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), applicationId, childMetricPath);
    final Response<List<AppdynamicsMetric>> response = request.execute();
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    if (response.isSuccessful()) {
      apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      final List<AppdynamicsMetric> allMetrices = response.body();
      for (Iterator<AppdynamicsMetric> iterator = allMetrices.iterator(); iterator.hasNext();) {
        final AppdynamicsMetric metric = iterator.next();

        // While getting the metric names we do not need to go to individual metrics names since the metric names in
        // each node are the same and there can be thousands of nodes in case of recycled nodes for container world
        // We would not be monitoring external calls metrics because one deployment is not going to effect multiple
        // tiers
        if (metric.getName().contains(INDIVIDUAL_NODES)) {
          iterator.remove();
          continue;
        }

        if (!includeExternal && metric.getName().contains(EXTERNAL_CALLS)) {
          iterator.remove();
          continue;
        }

        metric.setChildMetrices(getChildMetrics(appDynamicsConfig, applicationId, metric, childMetricPath,
            includeExternal, encryptionDetails, apiCallLog.copy()));
      }
      return allMetrices;
    } else {
      logger.info("Request not successful. Reason: {}", response);
      apiCallLog.addFieldToResponse(response.code(), response.errorBody().string(), FieldType.TEXT);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      throw new WingsException("Unsuccessful response while fetching data from AppDynamics. Error code: "
          + response.code() + ". Error message: " + response.errorBody());
    }
  }

  private List<AppdynamicsMetric> getExternalCallMetrics(AppDynamicsConfig appDynamicsConfig, long applicationId,
      AppdynamicsMetric appdynamicsMetric, String parentMetricPath, List<EncryptedDataDetail> encryptionDetails,
      ThirdPartyApiCallLog apiCallLog) throws IOException, CloneNotSupportedException {
    Preconditions.checkNotNull(apiCallLog);
    if (appdynamicsMetric.getType() != AppdynamicsMetricType.folder) {
      return Collections.emptyList();
    }

    final String childMetricPath = parentMetricPath + appdynamicsMetric.getName() + "|";
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("url")
                                     .value(appDynamicsConfig.getControllerUrl() + "/rest/applications/" + applicationId
                                         + "/metrics?output=JSON&metric-path=" + childMetricPath)
                                     .type(FieldType.URL)
                                     .build());
    apiCallLog.setTitle("Fetching external calls metric names from " + appDynamicsConfig.getControllerUrl());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    Call<List<AppdynamicsMetric>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listMetrices(
                getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), applicationId, childMetricPath);
    final Response<List<AppdynamicsMetric>> response = request.execute();
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    if (response.isSuccessful()) {
      apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      final List<AppdynamicsMetric> allMetrices = response.body();
      for (Iterator<AppdynamicsMetric> iterator = allMetrices.iterator(); iterator.hasNext();) {
        final AppdynamicsMetric metric = iterator.next();

        if (!metric.getName().contains(EXTERNAL_CALLS)) {
          iterator.remove();
          continue;
        }

        metric.setChildMetrices(getChildMetrics(
            appDynamicsConfig, applicationId, metric, childMetricPath, true, encryptionDetails, apiCallLog.copy()));
      }
      return allMetrices;
    } else {
      logger.info("Request not successful. Reason: {}", response);
      apiCallLog.addFieldToResponse(response.code(), response.errorBody().string(), FieldType.TEXT);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      throw new WingsException("Unsuccessful response while fetching data from AppDynamics. Error code: "
          + response.code() + ". Error message: " + response.errorBody());
    }
  }

  @Override
  public boolean validateConfig(AppDynamicsConfig appDynamicsConfig) throws IOException {
    if (!validUrl(appDynamicsConfig.getControllerUrl())) {
      throw new WingsException("AppDynamics Controller URL must be a valid URL");
    }
    Response<List<NewRelicApplication>> response = null;
    try {
      final Call<List<NewRelicApplication>> request =
          getAppdynamicsRestClient(appDynamicsConfig)
              .listAllApplications(getHeaderWithCredentials(appDynamicsConfig, Collections.emptyList()));
      response = request.execute();
      if (response.isSuccessful()) {
        return true;
      }
    } catch (Exception exception) {
      throw new WingsException("Could not reach AppDynamics server. " + Misc.getMessage(exception), exception);
    }

    final int errorCode = response.code();
    if (errorCode == HttpStatus.SC_UNAUTHORIZED) {
      throw new WingsException("Could not login to AppDynamics server with the given credentials");
    }

    throw new WingsException(response.message());
  }

  AppdynamicsRestClient getAppdynamicsRestClient(final AppDynamicsConfig appDynamicsConfig) {
    final Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(appDynamicsConfig.getControllerUrl().endsWith("/") ? appDynamicsConfig.getControllerUrl()
                                                                        : appDynamicsConfig.getControllerUrl() + "/")
            .addConverterFactory(JacksonConverterFactory.create())
            .client(Http.getOkHttpClientWithNoProxyValueSet(appDynamicsConfig.getControllerUrl()).build())
            .build();
    return retrofit.create(AppdynamicsRestClient.class);
  }

  private String getHeaderWithCredentials(
      AppDynamicsConfig appDynamicsConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(appDynamicsConfig, encryptionDetails);
    return "Basic "
        + Base64.encodeBase64String(
              String
                  .format("%s@%s:%s", appDynamicsConfig.getUsername(), appDynamicsConfig.getAccountname(),
                      new String(appDynamicsConfig.getPassword()))
                  .getBytes(StandardCharsets.UTF_8));
  }
}
