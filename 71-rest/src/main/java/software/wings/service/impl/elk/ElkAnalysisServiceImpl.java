package software.wings.service.impl.elk;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.delegatetasks.ElkLogzDataCollectionTask.parseElkResponse;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.apm.MLServiceUtil;
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
public class ElkAnalysisServiceImpl extends AnalysisServiceImpl implements ElkAnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(ElkAnalysisServiceImpl.class);

  @Inject private MLServiceUtil mlServiceUtil;

  @Override
  public Map<String, ElkIndexTemplate> getIndices(String accountId, String analysisServerConfigId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No elk setting with id: " + analysisServerConfigId + " found");
    }

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);

    final ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();
    SyncTaskContext elkTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
        .getIndices(elkConfig, encryptedDataDetails, null);
  }

  @Override
  public String getVersion(String accountId, ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException {
    SyncTaskContext elkTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
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
            .formattedQuery(elkSetupTestNodeData.isFormattedQuery())
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
        secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);
    SyncTaskContext elkTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    Object responseWithoutHost;
    try {
      responseWithoutHost = delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
                                .search((ElkConfig) settingAttribute.getValue(), encryptedDataDetails,
                                    elkFetchRequestWithoutHost, apiCallLogWithDummyStateExecution(accountId), 5);
    } catch (IOException ex) {
      logger.info("Error while getting data ", ex);
      return VerificationNodeDataSetupResponse.builder().providerReachable(false).build();
    }
    List<LogElement> logElements =
        parseElkResponse(responseWithoutHost, elkSetupTestNodeData.getQuery(), elkSetupTestNodeData.getTimeStampField(),
            elkSetupTestNodeData.getTimeStampFieldFormat(), elkSetupTestNodeData.getHostNameField(),
            elkSetupTestNodeData.getInstanceElement().getHostName(), elkSetupTestNodeData.getMessageField(), 0, false);
    if (logElements.isEmpty()) {
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(VerificationLoadResponse.builder().isLoadPresent(false).build())
          .build();
    }

    String hostName = mlServiceUtil.getHostNameFromExpression(elkSetupTestNodeData);
    logger.info("Hostname Expression : " + hostName);

    final ElkLogFetchRequest elkFetchRequestWithHost =
        ElkLogFetchRequest.builder()
            .query(elkSetupTestNodeData.getQuery())
            .formattedQuery(elkSetupTestNodeData.isFormattedQuery())
            .indices(elkSetupTestNodeData.getIndices())
            .hostnameField(hostName)
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
      responseWithHost = delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
                             .search((ElkConfig) settingAttribute.getValue(), encryptedDataDetails,
                                 elkFetchRequestWithHost, apiCallLogWithDummyStateExecution(accountId), 5);
    } catch (IOException ex) {
      logger.info("Error while getting data for node", ex);
      return VerificationNodeDataSetupResponse.builder().providerReachable(false).build();
    }
    parseElkResponse(responseWithHost, elkSetupTestNodeData.getQuery(), elkSetupTestNodeData.getTimeStampField(),
        elkSetupTestNodeData.getTimeStampFieldFormat(), hostName,
        elkSetupTestNodeData.getInstanceElement().getHostName(), elkSetupTestNodeData.getMessageField(), 0, false);

    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(VerificationLoadResponse.builder().loadResponse(responseWithoutHost).isLoadPresent(true).build())
        .dataForNode(responseWithHost)
        .build();
  }
}
