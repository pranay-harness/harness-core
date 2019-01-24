package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.intfc.aws.delegate.AwsRoute53HelperServiceDelegate;

import java.util.List;

@Singleton
public class AwsRoute53HelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsRoute53HelperServiceDelegate {
  @VisibleForTesting
  AmazonRoute53 getAmazonRoute53Client(
      String region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonRoute53ClientBuilder builder = AmazonRoute53ClientBuilder.standard().withRegion(region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return builder.build();
  }

  @Override
  public List<AwsRoute53HostedZoneData> listHostedZones(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonRoute53 client = getAmazonRoute53Client(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
      ListHostedZonesResult listHostedZonesResult = client.listHostedZones();
      List<HostedZone> hostedZones = listHostedZonesResult.getHostedZones();
      if (isNotEmpty(hostedZones)) {
        return hostedZones.stream()
            .map(zone
                -> AwsRoute53HostedZoneData.builder().hostedZoneId(zone.getId()).hostedZoneName(zone.getName()).build())
            .collect(toList());
      }
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public void upsertRoute53ParentRecord(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String parentRecordName, String parentRecordHostedZoneId, int blueServiceWeight, String blueServiceRecord,
      int greenServiceWeight, String greenServiceRecord, int ttl) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonRoute53 client = getAmazonRoute53Client(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
      ChangeResourceRecordSetsRequest changeResourceRecordSetsRequest =
          new ChangeResourceRecordSetsRequest()
              .withHostedZoneId(parentRecordHostedZoneId)
              .withChangeBatch(new ChangeBatch().withChanges(
                  new Change().withAction("UPSERT").withResourceRecordSet(
                      new ResourceRecordSet()
                          .withName(parentRecordName)
                          .withType("CNAME")
                          .withTTL(ttl > 0 ? (long) ttl : 60L)
                          .withSetIdentifier("Harness-Blue")
                          .withWeight((long) blueServiceWeight)
                          .withResourceRecords(new ResourceRecord().withValue(blueServiceRecord))),
                  new Change().withAction("UPSERT").withResourceRecordSet(
                      new ResourceRecordSet()
                          .withName(parentRecordName)
                          .withType("CNAME")
                          .withTTL(ttl > 0 ? (long) ttl : 60L)
                          .withSetIdentifier("Harness-Green")
                          .withWeight((long) greenServiceWeight)
                          .withResourceRecords(new ResourceRecord().withValue(greenServiceRecord)))));
      client.changeResourceRecordSets(changeResourceRecordSetsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }
}