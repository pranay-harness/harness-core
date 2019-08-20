package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.DURATION_TO_ASK_MINUTES;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.RETRIES;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.getUnsafeHttpClient;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.appdynamics.AppdynamicsRestClient;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.appdynamics.AppdynamicsMetric.AppdynamicsMetricType;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by rsingh on 4/17/17.
 */
@Singleton
@Slf4j
public class AppdynamicsDelegateServiceImpl implements AppdynamicsDelegateService {
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
      return response.body()
          .stream()
          .sorted(Comparator.comparing(application -> application.getName()))
          .collect(Collectors.toList());
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
      return response.body().stream().sorted(Comparator.comparing(tier -> tier.getName())).collect(Collectors.toSet());
    } else {
      logger.info("Request not successful. Reason: {}", response);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, USER)
          .addParam("reason", "could not fetch Appdynamics tiers " + response.message());
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
                    encryptionDetails, createApiCallLog(appDynamicsConfig.getAccountId(), null));
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
  public Set<AppdynamicsNode> getNodes(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) throws IOException {
    Preconditions.checkNotNull(apiCallLog);
    apiCallLog.setTitle("Fetching node list for app: " + appdynamicsAppId + " tier: " + tierId);
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    final Call<List<AppdynamicsNode>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listNodes(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId, tierId);
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("utl")
                                     .type(FieldType.URL)
                                     .value(request.request().url().toString())
                                     .build());
    Response<List<AppdynamicsNode>> response;
    try {
      response = request.execute();
    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
          .addParam("reason",
              "Unsuccessful response while fetching nodes from AppDynamics. Error message: " + e.getMessage()
                  + " Request: " + request.request().url());
    }
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    if (response.isSuccessful()) {
      apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      return response.body().stream().collect(Collectors.toSet());
    } else {
      apiCallLog.addFieldToResponse(response.code(), response.errorBody().string(), FieldType.TEXT);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      logger.error("Request not successful. Reason: {}", response);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
          .addParam("reason",
              "Unsuccessful response while fetching nodesfrom AppDynamics. Error code: " + response.code()
                  + ". Error:" + response.errorBody());
    }
  }

  @Override
  public List<AppdynamicsMetric> getTierBTMetrics(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      long tierId, List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) throws IOException {
    Preconditions.checkNotNull(apiCallLog);
    final AppdynamicsTier tier =
        getAppdynamicsTier(appDynamicsConfig, appdynamicsAppId, tierId, encryptionDetails, apiCallLog.copy());
    final String tierBTsPath = BT_PERFORMANCE_PATH_PREFIX + tier.getName();
    apiCallLog.setTitle("Fetching business transactions for tier from " + appDynamicsConfig.getControllerUrl());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    Call<List<AppdynamicsMetric>> tierBTMetricRequest =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listMetrices(
                getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId, tierBTsPath);
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("url")
                                     .value(tierBTMetricRequest.request().url().toString())
                                     .type(FieldType.URL)
                                     .build());

    Response<List<AppdynamicsMetric>> tierBTResponse;

    try {
      tierBTResponse = tierBTMetricRequest.execute();
    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
          .addParam("reason",
              "Unsuccessful response while fetching data from AppDynamics. Error message: " + e.getMessage()
                  + " Request: " + tierBTsPath);
    }
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
      String tierName, String btName, String hostName, Long startTime, Long endTime,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) {
    Preconditions.checkNotNull(apiCallLog);
    for (int i = 1; i <= RETRIES; i++) {
      apiCallLog = apiCallLog.copy();
      logger.info("getting AppDynamics metric data for state {}  host {}, app {}, tier {}, bt {}",
          apiCallLog.getStateExecutionId(), hostName, appdynamicsAppId, tierName, btName);
      String metricPath = BT_PERFORMANCE_PATH_PREFIX + tierName + "|" + btName + "|";

      metricPath += isEmpty(hostName) ? "*" : "Individual Nodes|" + hostName + "|*";
      logger.info("fetching metrics for path {} ", metricPath);

      apiCallLog.setTitle("Fetching metric data for " + metricPath);
      apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      try {
        Call<List<AppdynamicsMetricData>> tierBTMetricRequest;

        if (startTime != null && endTime != null) {
          tierBTMetricRequest =
              getAppdynamicsRestClient(appDynamicsConfig)
                  .getMetricDataTimeRange(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails),
                      appdynamicsAppId, metricPath, startTime, endTime);
          apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                           .name("url")
                                           .value(tierBTMetricRequest.request().url().toString())
                                           .type(FieldType.URL)
                                           .build());
        } else {
          throw new WingsException(
              "Start time or end Time was null while getting data from AppDynamics. StateExecutionId: "
              + apiCallLog.getStateExecutionId());
        }

        Response<List<AppdynamicsMetricData>> tierBTMResponse = tierBTMetricRequest.execute();
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
        if (tierBTMResponse.isSuccessful()) {
          apiCallLog.addFieldToResponse(tierBTMResponse.code(), tierBTMResponse.body(), FieldType.JSON);
          delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
          logger.info("for {} got {} metrics for path {}", apiCallLog.getStateExecutionId(),
              tierBTMResponse.body().size(), metricPath);
          return tierBTMResponse.body();
        } else {
          logger.info(
              "Request not successful for state {}. Reason: {}", apiCallLog.getStateExecutionId(), tierBTMResponse);
          apiCallLog.addFieldToResponse(tierBTMResponse.code(), tierBTMResponse.errorBody().string(), FieldType.TEXT);
          delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
          throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
              .addParam("reason",
                  "Unsuccessful response while fetching data from AppDynamics. Error code: " + tierBTMResponse.code()
                      + ". Error message: " + tierBTMResponse.errorBody());
        }
      } catch (Exception e) {
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
        apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
        delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
        if (i >= RETRIES) {
          throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
              .addParam("reason",
                  "Unsuccessful response while fetching data from AppDynamics. Error message: " + e.getMessage()
                      + " Request: " + metricPath);
        } else {
          sleep(Duration.ofSeconds(2));
        }
      }
    }
    throw new IllegalStateException("This state for " + apiCallLog.getStateExecutionId() + " should never reach");
  }

  public AppdynamicsTier getAppdynamicsTier(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) throws IOException {
    Preconditions.checkNotNull(apiCallLog);
    try {
      apiCallLog.setTitle("Fetching tiers from " + appDynamicsConfig.getControllerUrl());
      apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      final Call<List<AppdynamicsTier>> tierDetail =
          getAppdynamicsRestClient(appDynamicsConfig)
              .getTierDetails(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId, tierId);
      apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                       .name("url")
                                       .value(tierDetail.request().url().toString())
                                       .type(FieldType.URL)
                                       .build());
      final Response<List<AppdynamicsTier>> tierResponse = tierDetail.execute();
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(tierResponse.code(), tierResponse.body(), FieldType.JSON);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      if (!tierResponse.isSuccessful()) {
        logger.info("Request not successful. Reason: {}", tierResponse);
        throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
            .addParam("reason",
                "Unsuccessful response while fetching data from AppDynamics. Error code: " + tierResponse.code()
                    + ". Error message: " + tierResponse.errorBody());
      }
      return tierResponse.body().get(0);
    } catch (Exception e) {
      logger.info("Error while getting tier", e);
      if (!(e instanceof WingsException)) {
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
        apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
        delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      }
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
          .addParam("reason",
              "Unsuccessful response while fetching tiers from AppDynamics. Error message: " + e.getMessage());
    }
  }

  private List<AppdynamicsMetric> getChildMetrics(AppDynamicsConfig appDynamicsConfig, long applicationId,
      AppdynamicsMetric appdynamicsMetric, String parentMetricPath, boolean includeExternal,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) throws IOException {
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
    Response<List<AppdynamicsMetric>> response;

    try {
      response = request.execute();
    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
          .addParam("reason",
              "Unsuccessful response while fetching data from AppDynamics. Error message: " + e.getMessage()
                  + " Request: " + childMetricPath);
    }
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
    final Response<List<AppdynamicsMetric>> response;
    try {
      response = request.execute();
    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      delegateLogService.save(appDynamicsConfig.getAccountId(), apiCallLog);
      throw new WingsException(ErrorCode.APPDYNAMICS_ERROR)
          .addParam("reason",
              "Unsuccessful response while fetching data from AppDynamics. Error message: " + e.getMessage()
                  + " Request: " + childMetricPath);
    }

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
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(AppDynamicsConfig appDynamicsConfig,
      List<EncryptedDataDetail> encryptionDetails, AppdynamicsSetupTestNodeData setupTestNodeData, String hostName,
      ThirdPartyApiCallLog apiCallLog) throws IOException {
    final AppdynamicsTier tier = getAppdynamicsTier(appDynamicsConfig, setupTestNodeData.getApplicationId(),
        setupTestNodeData.getTierId(), encryptionDetails, apiCallLog.copy());
    final List<AppdynamicsMetric> tierMetrics = getTierBTMetrics(appDynamicsConfig,
        setupTestNodeData.getApplicationId(), setupTestNodeData.getTierId(), encryptionDetails, apiCallLog);

    if (isEmpty(tierMetrics)) {
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(VerificationLoadResponse.builder().isLoadPresent(false).build())
          .build();
    }
    final SortedSet<AppdynamicsMetricData> metricsData = new TreeSet<>();
    if (setupTestNodeData.isServiceLevel()) {
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(VerificationLoadResponse.builder().isLoadPresent(true).loadResponse(tierMetrics).build())
          .dataForNode(metricsData)
          .build();
    }
    List<Callable<List<AppdynamicsMetricData>>> callables = new ArrayList<>();
    long endTime = System.currentTimeMillis();
    long startTime = endTime - TimeUnit.MINUTES.toSeconds(DURATION_TO_ASK_MINUTES);
    for (AppdynamicsMetric appdynamicsMetric : tierMetrics) {
      callables.add(()
                        -> getTierBTMetricData(appDynamicsConfig, setupTestNodeData.getApplicationId(), tier.getName(),
                            appdynamicsMetric.getName(), hostName, startTime, endTime, encryptionDetails, apiCallLog));
    }
    List<Optional<List<AppdynamicsMetricData>>> results = dataCollectionService.executeParrallel(callables);
    results.forEach(result -> {
      if (result.isPresent()) {
        metricsData.addAll(result.get());
      }
    });

    for (Iterator<AppdynamicsMetricData> iterator = metricsData.iterator(); iterator.hasNext();) {
      AppdynamicsMetricData appdynamicsMetricData = iterator.next();
      String metricName = appdynamicsMetricData.getMetricName();
      if (metricName.contains("|")) {
        metricName = metricName.substring(metricName.lastIndexOf('|') + 1);
      }
      if (!AppdynamicsTimeSeries.getMetricsToTrack().contains(metricName)) {
        iterator.remove();
      }
    }
    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(VerificationLoadResponse.builder().isLoadPresent(true).loadResponse(tierMetrics).build())
        .dataForNode(metricsData)
        .build();
  }

  @Override
  public boolean validateConfig(AppDynamicsConfig appDynamicsConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    Response<List<NewRelicApplication>> response;
    try {
      final Call<List<NewRelicApplication>> request =
          getAppdynamicsRestClient(appDynamicsConfig)
              .listAllApplications(getHeaderWithCredentials(appDynamicsConfig, encryptedDataDetails));
      response = request.execute();
      if (response.isSuccessful()) {
        return true;
      }
    } catch (Exception exception) {
      throw new WingsException(
          "Could not reach AppDynamics server. " + ExceptionUtils.getMessage(exception), exception);
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
            .client(getUnsafeHttpClient(appDynamicsConfig.getControllerUrl()))
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
