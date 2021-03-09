package software.wings.service.impl.aws.delegate;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.util.Collections.singletonList;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.service.impl.delegate.AwsEcrApiHelperServiceDelegateImpl;
import software.wings.service.intfc.aws.delegate.AwsEcrHelperServiceDelegate;
import software.wings.service.mappers.artifact.AwsConfigToInternalMapper;

import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(CDC)
@Singleton
@TargetModule(Module._930_DELEGATE_TASKS)
public class AwsEcrHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsEcrHelperServiceDelegate {
  @Inject AwsEcrApiHelperServiceDelegateImpl awsEcrApiHelperServiceDelegateImpl;

  @VisibleForTesting
  AmazonECRClient getAmazonEcrClient(AwsConfig awsConfig, String region) {
    return awsEcrApiHelperServiceDelegateImpl.getAmazonEcrClient(
        AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig), region);
  }

  @Override
  public String getEcrImageUrl(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String imageName) {
    encryptionService.decrypt(awsConfig, encryptionDetails, false);
    return awsEcrApiHelperServiceDelegateImpl.getEcrImageUrl(
        AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig), region, imageName);
  }

  @Override
  public String getAmazonEcrAuthToken(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String awsAccount, String region) {
    encryptionService.decrypt(awsConfig, encryptionDetails, false);
    AmazonECRClient ecrClient = getAmazonEcrClient(awsConfig, region);
    tracker.trackECRCall("Get Ecr Auth Token");
    return ecrClient
        .getAuthorizationToken(new GetAuthorizationTokenRequest().withRegistryIds(singletonList(awsAccount)))
        .getAuthorizationData()
        .get(0)
        .getAuthorizationToken();
  }
}
