package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet;

import static software.wings.beans.SettingAttribute.SettingCategory.CE_CONNECTOR;
import static software.wings.settings.SettingVariableTypes.CE_AWS;

import com.google.inject.Singleton;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.cloudevents.aws.ecs.service.intfc.AwsECSClusterService;
import io.harness.ccm.setup.CECloudAccountDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECloudAccount;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.List;

@Slf4j
@Singleton
public class AwsECSClusterSyncTasklet implements Tasklet {
  @Autowired private AwsECSClusterService awsECSClusterService;
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
      CECloudAccount cloudAccount = CECloudAccount.builder()
                                        .accountId(accountId)
                                        .infraAccountId(ceAwsConfig.getAwsMasterAccountId())
                                        .awsCrossAccountAttributes(ceAwsConfig.getAwsCrossAccountAttributes())
                                        .build();
      awsECSClusterService.syncCEClusters(cloudAccount);
      List<CECloudAccount> ceCloudAccounts =
          ceCloudAccountDao.getByMasterAccountId(accountId, ceConnector.getUuid(), ceAwsConfig.getAwsMasterAccountId());
      ceCloudAccounts.forEach(ceCloudAccount -> awsECSClusterService.syncCEClusters(ceCloudAccount));
    });
    return null;
  }
}
