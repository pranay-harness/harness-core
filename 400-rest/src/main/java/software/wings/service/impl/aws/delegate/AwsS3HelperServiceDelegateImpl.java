package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.service.intfc.aws.delegate.AwsS3HelperServiceDelegate;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class AwsS3HelperServiceDelegateImpl extends AwsHelperServiceDelegateBase implements AwsS3HelperServiceDelegate {
  @VisibleForTesting
  AmazonS3Client getAmazonS3Client(AwsConfig awsConfig) {
    // S3 does not have region selection
    AmazonS3ClientBuilder builder =
        AmazonS3ClientBuilder.standard().withRegion(getRegion(awsConfig)).withForceGlobalBucketAccessEnabled(true);
    attachCredentials(builder, awsConfig);
    return (AmazonS3Client) builder.build();
  }

  @Override
  public List<String> listBucketNames(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackS3Call("List Buckets");
      AmazonS3Client client = getAmazonS3Client(awsConfig);
      List<Bucket> buckets = client.listBuckets();
      if (isEmpty(buckets)) {
        return emptyList();
      }
      return buckets.stream().map(Bucket::getName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }
}
