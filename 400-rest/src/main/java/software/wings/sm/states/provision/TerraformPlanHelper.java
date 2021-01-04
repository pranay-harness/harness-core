package software.wings.sm.states.provision;

import io.harness.beans.SweepingOutputInstance;
import io.harness.encryptors.clients.AwsKmsEncryptor;
import io.harness.security.encryption.EncryptedRecordData;

import software.wings.api.TerraformPlanParam;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TerraformPlanHelper {
  @Inject protected SweepingOutputService sweepingOutputService;
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected SecretManager secretManager;
  @Inject protected AwsKmsEncryptor awsKmsEncryptor;

  public void saveEncryptedTfPlanToSweepingOutput(EncryptedRecordData data, ExecutionContext context, String planName) {
    EncryptedRecordData encryptedTfPlan = getEncryptedTfPlanFromSweepingOutput(context, planName);
    if (encryptedTfPlan != null) {
      deleteEncryptedTfPlanFromSweepingOutput(context, planName);
    }
    sweepingOutputService.save(context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
                                   .name(planName)
                                   .value(TerraformPlanParam.builder().encryptedRecordData(data).build())
                                   .build());
  }

  @VisibleForTesting
  EncryptedRecordData getEncryptedTfPlanFromSweepingOutput(ExecutionContext context, String planName) {
    SweepingOutputInstance sweepingOutputInstance =
        sweepingOutputService.find(context.prepareSweepingOutputInquiryBuilder().name(planName).build());

    if (sweepingOutputInstance != null) {
      return ((TerraformPlanParam) sweepingOutputInstance.getValue()).getEncryptedRecordData();
    }
    return null;
  }

  @VisibleForTesting
  void deleteEncryptedTfPlanFromSweepingOutput(ExecutionContext context, String planName) {
    SweepingOutputInstance sweepingOutputInstance =
        sweepingOutputService.find(context.prepareSweepingOutputInquiryBuilder().name(planName).build());
    if (sweepingOutputInstance != null) {
      // delete the plan reference from sweeping output if present
      sweepingOutputService.deleteById(context.getAppId(), sweepingOutputInstance.getUuid());
    }
  }
}
