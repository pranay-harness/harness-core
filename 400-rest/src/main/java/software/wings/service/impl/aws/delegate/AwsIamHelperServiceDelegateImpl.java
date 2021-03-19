package software.wings.service.impl.aws.delegate;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.service.intfc.aws.delegate.AwsIamHelperServiceDelegate;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@TargetModule(Module._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class AwsIamHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsIamHelperServiceDelegate {
  @VisibleForTesting
  AmazonIdentityManagementClient getAmazonIdentityManagementClient(AwsConfig awsConfig) {
    AmazonIdentityManagementClientBuilder builder =
        AmazonIdentityManagementClient.builder().withRegion(getRegion(awsConfig));
    attachCredentialsAndBackoffPolicy(builder, awsConfig);
    return (AmazonIdentityManagementClient) builder.build();
  }

  @Override
  public Map<String, String> listIAMRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      Map<String, String> result = new HashMap<>();
      String nextMarker = null;
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      do {
        AmazonIdentityManagementClient amazonIdentityManagementClient = getAmazonIdentityManagementClient(awsConfig);
        ListRolesRequest listRolesRequest = new ListRolesRequest().withMaxItems(400).withMarker(nextMarker);
        tracker.trackIAMCall("List Roles");
        ListRolesResult listRolesResult = amazonIdentityManagementClient.listRoles(listRolesRequest);
        listRolesResult.getRoles().forEach(role -> result.put(role.getArn(), role.getRoleName()));
        nextMarker = listRolesResult.getMarker();
      } while (nextMarker != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyMap();
  }

  @Override
  public List<String> listIamInstanceRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      List<String> result = new ArrayList<>();
      String nextMarker = null;
      ListInstanceProfilesRequest listInstanceProfilesRequest;
      ListInstanceProfilesResult listInstanceProfilesResult;
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      do {
        listInstanceProfilesRequest = new ListInstanceProfilesRequest().withMarker(nextMarker);
        tracker.trackIAMCall("List Instance Profiles");
        listInstanceProfilesResult =
            getAmazonIdentityManagementClient(awsConfig).listInstanceProfiles(listInstanceProfilesRequest);
        result.addAll(listInstanceProfilesResult.getInstanceProfiles()
                          .stream()
                          .map(InstanceProfile::getInstanceProfileName)
                          .collect(toList()));
        nextMarker = listInstanceProfilesResult.getMarker();
      } while (nextMarker != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }
}
