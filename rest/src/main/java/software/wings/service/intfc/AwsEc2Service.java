package software.wings.service.intfc;

import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by anubhaw on 6/17/18.
 */
public interface AwsEc2Service {
  @DelegateTaskType(TaskType.AWS_VALIDATE)
  boolean validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
}
