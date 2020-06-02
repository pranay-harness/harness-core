package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.VerificationConstants.DURATION_TO_ASK_MINUTES;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.getUnsafeHttpClient;
import static software.wings.service.impl.ThirdPartyApiCallLog.NO_STATE_EXECUTION_ID;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.AppdynamicsValidationResponse.AppdynamicsMetricValueValidationResponse;
import io.harness.cvng.beans.AppdynamicsValidationResponse.AppdynamicsValidationResponseBuilder;
import io.harness.cvng.core.CVNextGenConstants;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.models.ThirdPartyApiResponseStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.cv.RequestExecutor;
import software.wings.helpers.ext.appdynamics.AppdynamicsRestClient;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.appdynamics.AppdynamicsMetric.AppdynamicsMetricType;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.security.EncryptionService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import java.util.concurrent.atomic.AtomicReference;
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
  @Inject private RequestExecutor requestExecutor;

  @Override
  public List<NewRelicApplication> getAllApplications(
      AppDynamicsConfig appDynamicsConfig, List<EncryptedDataDetail> encryptionDetails) {
    final Call<List<NewRelicApplication>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listAllApplications(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails));
    List<NewRelicApplication> newRelicApplications = requestExecutor.executeRequest(request);

    if (isNotEmpty(newRelicApplications)) {
      newRelicApplications = newRelicApplications.stream()
                                 .filter(application -> isNotEmpty(application.getName()))
                                 .sorted(Comparator.comparing(NewRelicApplication::getName))
                                 .collect(Collectors.toList());
    }
    return newRelicApplications;
  }

  @Override
  public Set<AppdynamicsTier> getTiers(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog thirdPartyApiCallLog) {
    Preconditions.checkNotNull(thirdPartyApiCallLog);
    ThirdPartyApiCallLog apiCallLog = thirdPartyApiCallLog.copy();
    final Call<Set<AppdynamicsTier>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listTiers(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId);
    apiCallLog.setTitle("Fetching tiers for application " + appdynamicsAppId);
    final Set<AppdynamicsTier> response = requestExecutor.executeRequest(apiCallLog, request);
    response.forEach(tier -> tier.setExternalTiers(new HashSet<>()));
    return new HashSet<>(response);
  }

  @Override
  public Set<AppdynamicsTier> getTierDependencies(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) {
    Preconditions.checkNotNull(apiCallLog);
    Set<AppdynamicsTier> tiers = getTiers(appDynamicsConfig, appdynamicsAppId, encryptionDetails, apiCallLog);
    List<Callable<Void>> callables = new ArrayList<>();
    tiers.forEach(tier -> callables.add(() -> {
      final String tierBTsPath = BT_PERFORMANCE_PATH_PREFIX + tier.getName();
      Call<List<AppdynamicsMetric>> tierBTMetricRequest =
          getAppdynamicsRestClient(appDynamicsConfig)
              .listMetrices(
                  getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId, tierBTsPath);

      List<AppdynamicsMetric> tierBtMetrics = requestExecutor.executeRequest(tierBTMetricRequest);
      tierBtMetrics.forEach(tierBtMetric -> {
        List<AppdynamicsMetric> externalCallMetrics =
            getExternalCallMetrics(appDynamicsConfig, appdynamicsAppId, tierBtMetric, tierBTsPath + "|",
                encryptionDetails, createApiCallLog(appDynamicsConfig.getAccountId(), null));
        externalCallMetrics.forEach(
            externalCallMetric -> parseAndAddExternalTier(tier.getExternalTiers(), externalCallMetric, tiers));
      });
      return null;
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
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) {
    Preconditions.checkNotNull(apiCallLog);
    apiCallLog.setTitle("Fetching node list for app: " + appdynamicsAppId + " tier: " + tierId);
    final Call<List<AppdynamicsNode>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listNodes(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId, tierId);
    return new HashSet<>(requestExecutor.executeRequest(apiCallLog, request));
  }

  @Override
  public List<AppdynamicsMetric> getTierBTMetrics(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      long tierId, List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) {
    Preconditions.checkNotNull(apiCallLog);
    final AppdynamicsTier tier =
        getAppdynamicsTier(appDynamicsConfig, appdynamicsAppId, tierId, encryptionDetails, apiCallLog);
    final String tierBTsPath = BT_PERFORMANCE_PATH_PREFIX + tier.getName();
    apiCallLog.setTitle("Fetching business transactions for tier from " + appDynamicsConfig.getControllerUrl());
    Call<List<AppdynamicsMetric>> tierBTMetricRequest =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listMetrices(
                getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId, tierBTsPath);

    return requestExecutor.executeRequest(apiCallLog, tierBTMetricRequest);
  }

  @Override
  public List<AppdynamicsMetricData> getTierBTMetricData(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      String tierName, String btName, String hostName, Long startTime, Long endTime,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) {
    Preconditions.checkNotNull(apiCallLog);
    Preconditions.checkNotNull(startTime, "Start time was null while getting data from Appdynamics. StateExecutionId: ",
        apiCallLog.getStateExecutionId());
    Preconditions.checkNotNull(endTime,
        "End time was null while getting data from Appdynamics. StateExecutionId: ", apiCallLog.getStateExecutionId());

    String metricPath = BT_PERFORMANCE_PATH_PREFIX + tierName + "|" + btName + "|"
        + (isEmpty(hostName) ? "*" : "Individual Nodes|" + hostName + "|*");
    apiCallLog.setTitle("Fetching metric data for " + metricPath);
    logger.debug("fetching metrics for path {} ", metricPath);
    Call<List<AppdynamicsMetricData>> tierBTMetricRequest =
        getAppdynamicsRestClient(appDynamicsConfig)
            .getMetricDataTimeRange(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId,
                metricPath, startTime, endTime, false);
    return requestExecutor.executeRequest(apiCallLog, tierBTMetricRequest);
  }

  @Override
  public AppdynamicsTier getAppdynamicsTier(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) {
    Preconditions.checkNotNull(apiCallLog);
    apiCallLog.setTitle("Fetching tiers from " + appDynamicsConfig.getControllerUrl());
    final Call<List<AppdynamicsTier>> tierDetail =
        getAppdynamicsRestClient(appDynamicsConfig)
            .getTierDetails(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdynamicsAppId, tierId);
    List<AppdynamicsTier> tiers = requestExecutor.executeRequest(apiCallLog, tierDetail);
    return tiers.get(0);
  }

  private List<AppdynamicsMetric> getChildMetrics(AppDynamicsConfig appDynamicsConfig, long applicationId,
      AppdynamicsMetric appdynamicsMetric, String parentMetricPath, boolean includeExternal,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) {
    Preconditions.checkNotNull(apiCallLog);
    if (appdynamicsMetric.getType() != AppdynamicsMetricType.folder) {
      return Collections.emptyList();
    }

    if (parentMetricPath.contains("|" + appdynamicsMetric.getName() + "|")) {
      return Collections.emptyList();
    }

    final String childMetricPath = parentMetricPath + appdynamicsMetric.getName() + "|";
    apiCallLog.setTitle("Fetching metric names from " + appDynamicsConfig.getControllerUrl());
    Call<List<AppdynamicsMetric>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listMetrices(
                getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), applicationId, childMetricPath);
    final List<AppdynamicsMetric> allMetrics = requestExecutor.executeRequest(apiCallLog, request);
    for (Iterator<AppdynamicsMetric> iterator = allMetrics.iterator(); iterator.hasNext();) {
      final AppdynamicsMetric metric = iterator.next();

      // While getting the metric names we do not need to go to individual metrics names since the metric names in
      // each node are the same and there can be thousands of nodes in case of recycled nodes for container world
      // We would not be monitoring external calls metrics because one deployment is not going to effect multiple
      // tiers
      if (metric.getName().contains(INDIVIDUAL_NODES)
          || !includeExternal && metric.getName().contains(EXTERNAL_CALLS)) {
        iterator.remove();
        continue;
      }

      metric.setChildMetrices(getChildMetrics(appDynamicsConfig, applicationId, metric, childMetricPath,
          includeExternal, encryptionDetails, apiCallLog.copy()));
    }
    return allMetrics;
  }

  private List<AppdynamicsMetric> getExternalCallMetrics(AppDynamicsConfig appDynamicsConfig, long applicationId,
      AppdynamicsMetric appdynamicsMetric, String parentMetricPath, List<EncryptedDataDetail> encryptionDetails,
      ThirdPartyApiCallLog apiCallLog) {
    Preconditions.checkNotNull(apiCallLog);
    if (appdynamicsMetric.getType() != AppdynamicsMetricType.folder) {
      return Collections.emptyList();
    }

    final String childMetricPath = parentMetricPath + appdynamicsMetric.getName() + "|";
    apiCallLog.setTitle("Fetching external calls metric names from " + appDynamicsConfig.getControllerUrl());
    Call<List<AppdynamicsMetric>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listMetrices(
                getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), applicationId, childMetricPath);
    final List<AppdynamicsMetric> allMetrics = requestExecutor.executeRequest(apiCallLog, request);
    for (Iterator<AppdynamicsMetric> iterator = allMetrics.iterator(); iterator.hasNext();) {
      final AppdynamicsMetric metric = iterator.next();
      if (!metric.getName().contains(EXTERNAL_CALLS)) {
        iterator.remove();
        continue;
      }
      metric.setChildMetrices(getChildMetrics(
          appDynamicsConfig, applicationId, metric, childMetricPath, true, encryptionDetails, apiCallLog.copy()));
    }
    return allMetrics;
  }

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(AppDynamicsConfig appDynamicsConfig,
      List<EncryptedDataDetail> encryptionDetails, AppdynamicsSetupTestNodeData setupTestNodeData, String hostName,
      ThirdPartyApiCallLog apiCallLog) {
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
    long startTime = endTime - TimeUnit.MINUTES.toMillis(DURATION_TO_ASK_MINUTES);
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
  public Set<AppdynamicsValidationResponse> getMetricPackData(AppDynamicsConfig appDynamicsConfig,
      List<EncryptedDataDetail> encryptionDetails, long appdAppId, long appdTierId, String requestGuid,
      List<MetricPack> metricPacks, Instant startTime, Instant endTime) {
    final AppdynamicsTier appdynamicsTier = getAppdynamicsTier(appDynamicsConfig, appdAppId, appdTierId,
        encryptionDetails, createApiCallLog(appDynamicsConfig.getAccountId(), NO_STATE_EXECUTION_ID));
    Preconditions.checkNotNull(appdynamicsTier, "No tier found with id {} for app {}", appdTierId, appdAppId);

    Set<AppdynamicsValidationResponse> appdynamicsValidationResponses = new HashSet<>();
    metricPacks.forEach(metricPack -> {
      final String metricPackName = metricPack.getName();
      final AppdynamicsValidationResponseBuilder appdynamicsValidationResponse =
          AppdynamicsValidationResponse.builder().metricPackName(metricPackName);
      List<Callable<AppdynamicsMetricValueValidationResponse>> callables = new ArrayList<>();
      metricPack.getMetrics()
          .stream()
          .filter(metricDefinition -> metricDefinition.isIncluded() && isNotEmpty(metricDefinition.getValidationPath()))
          .forEach(metricDefinition -> callables.add(() -> {
            String metricPath = metricDefinition.getValidationPath().replaceAll(
                CVNextGenConstants.APPD_TIER_ID_PLACEHOLDER, appdynamicsTier.getName());
            Call<List<AppdynamicsMetricData>> metriDataRequest =
                getAppdynamicsRestClient(appDynamicsConfig)
                    .getMetricDataTimeRange(getHeaderWithCredentials(appDynamicsConfig, encryptionDetails), appdAppId,
                        metricPath, startTime.toEpochMilli(), endTime.toEpochMilli(), true);
            try {
              List<AppdynamicsMetricData> appdynamicsMetricData = requestExecutor.executeRequest(
                  ThirdPartyApiCallLog.createApiCallLog(appDynamicsConfig.getAccountId(),
                      metricPackName + ":" + metricDefinition.getName() + ":" + requestGuid),
                  metriDataRequest);

              if (isEmpty(appdynamicsMetricData) || isEmpty(appdynamicsMetricData.get(0).getMetricValues())) {
                return AppdynamicsMetricValueValidationResponse.builder()
                    .metricName(metricDefinition.getName())
                    .apiResponseStatus(ThirdPartyApiResponseStatus.NO_DATA)
                    .build();
              }

              return AppdynamicsMetricValueValidationResponse.builder()
                  .metricName(metricDefinition.getName())
                  .apiResponseStatus(ThirdPartyApiResponseStatus.SUCCESS)
                  .value(appdynamicsMetricData.get(0).getMetricValues().get(0).getValue())
                  .build();
            } catch (Exception e) {
              logger.info("Exception while validating for request " + requestGuid, e);
              return AppdynamicsMetricValueValidationResponse.builder()
                  .metricName(metricDefinition.getName())
                  .apiResponseStatus(ThirdPartyApiResponseStatus.FAILED)
                  .errorMessage(ExceptionUtils.getMessage(e))
                  .build();
            }
          }));
      final List<Optional<AppdynamicsMetricValueValidationResponse>> metricValidationResponses =
          dataCollectionService.executeParrallel(callables);
      AtomicReference<ThirdPartyApiResponseStatus> overAllStatus =
          new AtomicReference<>(ThirdPartyApiResponseStatus.SUCCESS);
      metricValidationResponses.forEach(validationResponse -> {
        if (validationResponse.isPresent()) {
          final AppdynamicsMetricValueValidationResponse valueValidationResponse = validationResponse.get();
          if (valueValidationResponse.getApiResponseStatus().compareTo(overAllStatus.get()) > 0) {
            overAllStatus.set(valueValidationResponse.getApiResponseStatus());
          }
          appdynamicsValidationResponse.addValidationResponse(valueValidationResponse);
        }
      });
      appdynamicsValidationResponses.add(appdynamicsValidationResponse.overallStatus(overAllStatus.get()).build());
    });

    return appdynamicsValidationResponses;
  }

  @Override
  public boolean validateConfig(AppDynamicsConfig appDynamicsConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    final Call<List<NewRelicApplication>> request =
        getAppdynamicsRestClient(appDynamicsConfig)
            .listAllApplications(getHeaderWithCredentials(appDynamicsConfig, encryptedDataDetails));
    requestExecutor.executeRequest(request);
    return true;
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
