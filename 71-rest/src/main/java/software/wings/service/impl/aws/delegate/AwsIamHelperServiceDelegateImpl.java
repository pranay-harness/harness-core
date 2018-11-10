package software.wings.service.impl.aws.delegate;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.aws.delegate.AwsIamHelperServiceDelegate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class AwsIamHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsIamHelperServiceDelegate {
  @VisibleForTesting
  AmazonIdentityManagementClient getAmazonIdentityManagementClient(
      String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonIdentityManagementClientBuilder builder =
        AmazonIdentityManagementClient.builder().withRegion(Regions.US_EAST_1);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonIdentityManagementClient) builder.build();
  }

  @Override
  public Map<String, String> listIAMRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      Map<String, String> result = new HashMap<>();
      String nextMarker = null;
      encryptionService.decrypt(awsConfig, encryptionDetails);
      do {
        AmazonIdentityManagementClient amazonIdentityManagementClient = getAmazonIdentityManagementClient(
            awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
        ListRolesRequest listRolesRequest = new ListRolesRequest().withMaxItems(400).withMarker(nextMarker);
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
      encryptionService.decrypt(awsConfig, encryptionDetails);
      do {
        listInstanceProfilesRequest = new ListInstanceProfilesRequest().withMarker(nextMarker);
        listInstanceProfilesResult = getAmazonIdentityManagementClient(
            awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
                                         .listInstanceProfiles(listInstanceProfilesRequest);
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