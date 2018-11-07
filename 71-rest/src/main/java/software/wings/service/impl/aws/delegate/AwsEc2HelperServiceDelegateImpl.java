package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ec2.model.Vpc;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class AwsEc2HelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsEc2HelperServiceDelegate {
  @VisibleForTesting
  AmazonEC2Client getAmazonEc2Client(String region, String accessKey, char[] secretKey) {
    return (AmazonEC2Client) AmazonEC2ClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  @Override
  public boolean validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      getAmazonEc2Client(Regions.US_EAST_1.getName(), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeRegions();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      if (amazonEC2Exception.getStatusCode() == 401) {
        return false;
      }
      handleAmazonServiceException(amazonEC2Exception);
    }
    return true;
  }

  @Override
  public List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client =
          getAmazonEc2Client(Regions.US_EAST_1.getName(), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEC2Client.describeRegions().getRegions().stream().map(Region::getRegionName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  @Override
  public List<String> listVPCs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEC2Client
          .describeVpcs(new DescribeVpcsRequest().withFilters(new Filter("state").withValues("available")))
          .getVpcs()
          .stream()
          .map(Vpc::getVpcId)
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  @Override
  public List<String> listSubnets(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      List<Filter> filters = new ArrayList<>();
      if (isNotEmpty(vpcIds)) {
        filters.add(new Filter("vpc-id", vpcIds));
      }
      filters.add(new Filter("state").withValues("available"));
      return amazonEC2Client.describeSubnets(new DescribeSubnetsRequest().withFilters(filters))
          .getSubnets()
          .stream()
          .map(Subnet::getSubnetId)
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  @Override
  public List<String> listSGs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    List<String> result = new ArrayList<>();
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      String nextToken = null;
      do {
        AmazonEC2Client amazonEC2Client =
            getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
        List<Filter> filters = new ArrayList<>();
        if (isNotEmpty(vpcIds)) {
          filters.add(new Filter("vpc-id", vpcIds));
        }
        DescribeSecurityGroupsResult describeSecurityGroupsResult = amazonEC2Client.describeSecurityGroups(
            new DescribeSecurityGroupsRequest().withNextToken(nextToken).withFilters(filters));
        List<SecurityGroup> securityGroups = describeSecurityGroupsResult.getSecurityGroups();
        result.addAll(securityGroups.stream().map(SecurityGroup::getGroupId).collect(toList()));
        nextToken = describeSecurityGroupsResult.getNextToken();
      } while (nextToken != null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return result;
  }

  @Override
  public Set<String> listTags(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String resourceType) {
    String nextToken = null;
    Set<String> tags = new HashSet<>();
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      do {
        AmazonEC2Client amazonEC2Client =
            getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
        DescribeTagsResult describeTagsResult =
            amazonEC2Client.describeTags(new DescribeTagsRequest()
                                             .withNextToken(nextToken)
                                             .withFilters(new Filter("resource-type").withValues(resourceType))
                                             .withMaxResults(1000));
        tags.addAll(describeTagsResult.getTags().stream().map(TagDescription::getKey).collect(Collectors.toSet()));
        nextToken = describeTagsResult.getNextToken();
      } while (nextToken != null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return tags;
  }

  @Override
  public List<Instance> listEc2Instances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<Filter> filters) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      List<Instance> result = new ArrayList<>();
      String nextToken = null;
      do {
        DescribeInstancesRequest describeInstancesRequest =
            new DescribeInstancesRequest().withNextToken(nextToken).withFilters(filters);
        DescribeInstancesResult describeInstancesResult =
            getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
                .describeInstances(describeInstancesRequest);
        result.addAll(getInstanceList(describeInstancesResult));
        nextToken = describeInstancesResult.getNextToken();
      } while (nextToken != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  @Override
  public List<Instance> listEc2Instances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, List<String> instanceIds, String region) {
    try {
      if (instanceIds.isEmpty()) {
        return emptyList();
      }
      encryptionService.decrypt(awsConfig, encryptionDetails);
      List<Instance> result = new ArrayList<>();
      String nextToken = null;
      do {
        DescribeInstancesRequest describeInstancesRequest =
            new DescribeInstancesRequest().withNextToken(nextToken).withInstanceIds(instanceIds);
        DescribeInstancesResult describeInstancesResult =
            getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
                .describeInstances(describeInstancesRequest);
        result.addAll(getInstanceList(describeInstancesResult));
        nextToken = describeInstancesResult.getNextToken();
      } while (nextToken != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  private List<Instance> getInstanceList(DescribeInstancesResult result) {
    List<Instance> instanceList = Lists.newArrayList();
    result.getReservations().forEach(reservation -> instanceList.addAll(reservation.getInstances()));
    return instanceList;
  }
}