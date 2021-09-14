/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet;

import static software.wings.beans.SettingAttribute.SettingCategory.CE_CONNECTOR;
import static software.wings.settings.SettingVariableTypes.CE_AWS;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.cloudevents.aws.ecs.service.intfc.AwsAccountService;
import io.harness.batch.processing.cloudevents.aws.ecs.service.intfc.AwsECSClusterService;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.setup.CECloudAccountDao;

import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class AwsECSClusterSyncTasklet implements Tasklet {
  @Autowired private AwsECSClusterService awsECSClusterService;
  @Autowired private AwsAccountService awsAccountService;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private CECloudAccountDao ceCloudAccountDao;
  private JobParameters parameters;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    List<SettingAttribute> ceConnectorList =
        cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(accountId, CE_CONNECTOR, CE_AWS);
    ceConnectorList.forEach(ceConnector -> {
      CEAwsConfig ceAwsConfig = (CEAwsConfig) ceConnector.getValue();
      syncLinkedAccounts(accountId, ceConnector.getAccountId(), ceAwsConfig);
      CECloudAccount cloudAccount = CECloudAccount.builder()
                                        .accountId(accountId)
                                        .infraAccountId(ceAwsConfig.getAwsMasterAccountId())
                                        .infraMasterAccountId(ceAwsConfig.getAwsMasterAccountId())
                                        .masterAccountSettingId(ceConnector.getUuid())
                                        .awsCrossAccountAttributes(ceAwsConfig.getAwsCrossAccountAttributes())
                                        .build();
      awsECSClusterService.syncCEClusters(cloudAccount);
      List<CECloudAccount> ceCloudAccounts =
          ceCloudAccountDao.getByMasterAccountId(accountId, ceConnector.getUuid(), ceAwsConfig.getAwsMasterAccountId());
      ceCloudAccounts.forEach(ceCloudAccount -> awsECSClusterService.syncCEClusters(ceCloudAccount));
    });
    return null;
  }

  private void syncLinkedAccounts(String accountId, String settingId, CEAwsConfig ceAwsConfig) {
    try {
      log.info("Started syncing linked accounts");
      awsAccountService.syncLinkedAccounts(accountId, settingId, ceAwsConfig);
      log.info("Finished syncing linked accounts");
    } catch (Exception ex) {
      log.error("Exception while sync account", ex);
    }
  }
}
