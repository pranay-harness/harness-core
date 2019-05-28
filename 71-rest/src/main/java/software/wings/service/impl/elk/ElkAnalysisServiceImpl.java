package software.wings.service.impl.elk;

import static io.harness.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.delegatetasks.ElkLogzDataCollectionTask.parseElkResponse;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.apm.MLServiceUtils;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 8/23/17.
 */
@Singleton
@Slf4j
public class ElkAnalysisServiceImpl extends AnalysisServiceImpl implements ElkAnalysisService {
  @Inject private MLServiceUtils mlServiceUtils;

  @Override
  public Map<String, ElkIndexTemplate> getIndices(String accountId, String analysisServerConfigId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No elk setting with id: " + analysisServerConfigId + " found");
    }

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);

    final ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();
    SyncTaskContext elkTaskContext = SyncTaskContext.builder()
                                         .accountId(settingAttribute.getAccountId())
                                         .appId(GLOBAL_APP_ID)
                                         .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                         .build();
    return delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
        .getIndices(elkConfig, encryptedDataDetails, null);
  }

  @Override
  public String getVersion(String accountId, ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException {
    SyncTaskContext elkTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    return delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
        .getVersion(elkConfig, encryptedDataDetails);
  }

  @Override
  public VerificationNodeDataSetupResponse getLogDataByHost(
      final String accountId, final ElkSetupTestNodeData elkSetupTestNodeData) {
    logger.info("Starting Log Data collection by Host for account Id : {}, ElkSetupTestNodeData : {}", accountId,
        elkSetupTestNodeData);
    // gets the settings attributes for given settings id
    final SettingAttribute settingAttribute = settingsService.get(elkSetupTestNodeData.getSettingId());
    logger.info("Settings attribute : " + settingAttribute);
    if (settingAttribute == null) {
      throw new WingsException(
          "No " + StateType.ELK + " setting with id: " + elkSetupTestNodeData.getSettingId() + " found");
    }

    final ElkLogFetchRequest elkFetchRequestWithoutHost =
        ElkLogFetchRequest.builder()
            .query(elkSetupTestNodeData.getQuery())
            .indices(elkSetupTestNodeData.getIndices())
            .hosts(Collections.EMPTY_SET)
            .hostnameField(elkSetupTestNodeData.getHostNameField())
            .messageField(elkSetupTestNodeData.getMessageField())
            .timestampField(elkSetupTestNodeData.getTimeStampField())
            .startTime(TimeUnit.SECONDS.toMillis(OffsetDateTime.now().minusMinutes(15).toEpochSecond()))
            .endTime(TimeUnit.SECONDS.toMillis(OffsetDateTime.now().toEpochSecond()))
            .queryType(elkSetupTestNodeData.getQueryType())
            .build();
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    SyncTaskContext elkTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    Object responseWithoutHost;
    try {
      responseWithoutHost =
          delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
              .search((ElkConfig) settingAttribute.getValue(), encryptedDataDetails, elkFetchRequestWithoutHost,
                  createApiCallLog(
                      settingAttribute.getAccountId(), elkSetupTestNodeData.getAppId(), elkSetupTestNodeData.getGuid()),
                  5);
    } catch (IOException ex) {
      logger.info("Error while getting data ", ex);
      return VerificationNodeDataSetupResponse.builder().providerReachable(false).build();
    }
    List<LogElement> logElementsWithoutHost = parseElkResponse(responseWithoutHost, elkSetupTestNodeData.getQuery(),
        elkSetupTestNodeData.getTimeStampField(), elkSetupTestNodeData.getTimeStampFieldFormat(),
        elkSetupTestNodeData.getHostNameField(),
        elkSetupTestNodeData.isServiceLevel() ? null : elkSetupTestNodeData.getInstanceElement().getHostName(),
        elkSetupTestNodeData.getMessageField(), 0, true, TimeUnit.SECONDS.toMillis(elkSetupTestNodeData.getFromTime()),
        TimeUnit.SECONDS.toMillis(elkSetupTestNodeData.getToTime()));

    if (elkSetupTestNodeData.isServiceLevel()) {
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(VerificationLoadResponse.builder()
                            .isLoadPresent(!logElementsWithoutHost.isEmpty())
                            .loadResponse(logElementsWithoutHost)
                            .build())
          .build();
    }

    if (logElementsWithoutHost.isEmpty()) {
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(VerificationLoadResponse.builder().isLoadPresent(false).build())
          .build();
    }

    String hostNameField = elkSetupTestNodeData.getHostNameField();
    String hostName = mlServiceUtils.getHostNameFromExpression(elkSetupTestNodeData);

    logger.info("Hostname Expression : " + hostName);
    final ElkLogFetchRequest elkFetchRequestWithHost =
        ElkLogFetchRequest.builder()
            .query(elkSetupTestNodeData.getQuery())
            .indices(elkSetupTestNodeData.getIndices())
            .hostnameField(hostNameField)
            .hosts(Collections.singleton(elkSetupTestNodeData.getInstanceElement().getHostName()))
            .messageField(elkSetupTestNodeData.getMessageField())
            .timestampField(elkSetupTestNodeData.getTimeStampField())
            .startTime(TimeUnit.SECONDS.toMillis(OffsetDateTime.now().minusMinutes(15).toEpochSecond()))
            .endTime(TimeUnit.SECONDS.toMillis(OffsetDateTime.now().toEpochSecond()))
            .queryType(elkSetupTestNodeData.getQueryType())
            .build();
    logger.info("ElkFetchRequest to be send : " + elkFetchRequestWithHost);
    Object responseWithHost;
    try {
      responseWithHost =
          delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
              .search((ElkConfig) settingAttribute.getValue(), encryptedDataDetails, elkFetchRequestWithHost,
                  createApiCallLog(
                      settingAttribute.getAccountId(), elkSetupTestNodeData.getAppId(), elkSetupTestNodeData.getGuid()),
                  5);
    } catch (IOException ex) {
      logger.info("Error while getting data for node", ex);
      return VerificationNodeDataSetupResponse.builder().providerReachable(false).build();
    }
    List<LogElement> logElementsWithHost = parseElkResponse(responseWithHost, elkSetupTestNodeData.getQuery(),
        elkSetupTestNodeData.getTimeStampField(), elkSetupTestNodeData.getTimeStampFieldFormat(), hostNameField,
        elkSetupTestNodeData.getInstanceElement().getHostName(), elkSetupTestNodeData.getMessageField(), 0, false, -1,
        -1);

    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(VerificationLoadResponse.builder()
                          .loadResponse(logElementsWithoutHost)
                          .isLoadPresent(!logElementsWithHost.isEmpty())
                          .build())
        .dataForNode(logElementsWithHost.isEmpty() ? null : logElementsWithoutHost)
        .build();
  }

  @Override
  public Boolean validateQuery(
      String accountId, String appId, String settingId, String query, String index, String guid) {
    try {
      // create a sample test node data for elk. And use it to validate Query from ELK server
      ElkSetupTestNodeData elkSetupTestNodeData =
          ElkSetupTestNodeData.builder()
              .settingId(settingId)
              .query(query)
              .indices(index)
              .appId(appId)
              .messageField("@timestamp")
              .hostNameField("beat.hostname")
              .timeStampField(query)
              .timeStampFieldFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
              .query(query)
              .queryType(ElkQueryType.MATCH)
              .instanceName("testHost")
              .guid(guid)
              .instanceElement(anInstanceElement()
                                   .withUuid("8cec1e1b0d16")
                                   .withDisplayName("8cec1e1b0d16")
                                   .withHostName("testHost")
                                   .withDockerId("8cec1e1b0d16")
                                   .withHost(aHostElement()
                                                 .withUuid("8cec1e1b0d16")
                                                 .withHostName("testHost")
                                                 .withIp("1.1.1.1")
                                                 .withInstanceId(null)
                                                 .withPublicDns(null)
                                                 .withEc2Instance(null)
                                                 .build())
                                   .withServiceTemplateElement(
                                       aServiceTemplateElement().withUuid("8cec1e1b0d16").withName(null).build())
                                   .withPodName("testHost")
                                   .withWorkloadName("testHost")
                                   .build())
              .isServiceLevel(true)
              .build();
      getLogDataByHost(accountId, elkSetupTestNodeData);
      logger.info("Valid query passed with query {} and index {}", query, index);
      return true;
    } catch (Exception ex) {
      throw new WingsException(ErrorCode.ELK_CONFIGURATION_ERROR, ex).addParam("reason", ExceptionUtils.getMessage(ex));
    }
  }
}
