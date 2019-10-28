package software.wings.service.impl.aws.delegate;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.AWSServiceDiscoveryClientBuilder;
import com.amazonaws.services.servicediscovery.model.GetNamespaceRequest;
import com.amazonaws.services.servicediscovery.model.GetNamespaceResult;
import com.amazonaws.services.servicediscovery.model.GetServiceRequest;
import com.amazonaws.services.servicediscovery.model.GetServiceResult;
import com.amazonaws.services.servicediscovery.model.Namespace;
import com.amazonaws.services.servicediscovery.model.Service;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.service.intfc.aws.delegate.AwsServiceDiscoveryHelperServiceDelegate;

import java.util.List;

@Singleton
public class AwsServiceDiscoveryHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsServiceDiscoveryHelperServiceDelegate {
  @VisibleForTesting
  AWSServiceDiscovery getAmazonServiceDiscoveryClient(String region, AwsConfig awsConfig) {
    AWSServiceDiscoveryClientBuilder builder = AWSServiceDiscoveryClientBuilder.standard().withRegion(region);
    attachCredentials(builder, awsConfig);
    return builder.build();
  }

  @Override
  public String getRecordValueForService(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String serviceId) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AWSServiceDiscovery client = getAmazonServiceDiscoveryClient(region, awsConfig);
      tracker.trackSDSCall("Get Service");
      GetServiceResult getServiceResult = client.getService(new GetServiceRequest().withId(serviceId));
      if (getServiceResult == null || getServiceResult.getService() == null) {
        return "";
      }
      Service service = getServiceResult.getService();
      String namespaceId = service.getDnsConfig().getNamespaceId();
      tracker.trackSDSCall("Get NameSpace");
      GetNamespaceResult getNamespaceResult = client.getNamespace(new GetNamespaceRequest().withId(namespaceId));
      if (getNamespaceResult == null || getNamespaceResult.getNamespace() == null) {
        return "";
      }
      Namespace namespace = getNamespaceResult.getNamespace();
      return format("%s.%s", service.getName(), namespace.getName());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return "";
  }
}