package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionRequest;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeLaunchTemplateVersionsRequest;
import com.amazonaws.services.ec2.model.DescribeLaunchTemplateVersionsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.LaunchTemplateVersion;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagDescription;
import io.harness.data.structure.CollectionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsSecurityGroup;
import software.wings.service.impl.aws.model.AwsSubnet;
import software.wings.service.impl.aws.model.AwsVPC;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Singleton
public class AwsEc2HelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsEc2HelperServiceDelegate {
  private static final String NAME = "Name";

  @VisibleForTesting
  AmazonEC2Client getAmazonEc2Client(String region, AwsConfig awsConfig) {
    AmazonEC2ClientBuilder builder = AmazonEC2ClientBuilder.standard().withRegion(region);
    attachCredentials(builder, awsConfig);
    return (AmazonEC2Client) builder.build();
  }

  @Override
  public boolean validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      tracker.trackEC2Call("Get Ec2 client");
      getAmazonEc2Client(Regions.US_EAST_1.getName(), awsConfig).describeRegions();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      if (amazonEC2Exception.getStatusCode() == 401) {
        return false;
      }
      handleAmazonServiceException(amazonEC2Exception);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return true;
  }

  @Override
  public List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(Regions.US_EAST_1.getName(), awsConfig);
      tracker.trackEC2Call("List Ec2 regions");
      return amazonEC2Client.describeRegions().getRegions().stream().map(Region::getRegionName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<AwsVPC> listVPCs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig);
      tracker.trackEC2Call("List VPCs");
      return amazonEC2Client
          .describeVpcs(new DescribeVpcsRequest().withFilters(new Filter("state").withValues("available")))
          .getVpcs()
          .stream()
          .map(vpc
              -> AwsVPC.builder()
                     .id(vpc.getVpcId())
                     .name(CollectionUtils.emptyIfNull(vpc.getTags())
                               .stream()
                               .filter(tag -> NAME.equals(tag.getKey()))
                               .findFirst()
                               .orElse(new Tag(NAME, ""))
                               .getValue())
                     .build())
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<AwsSubnet> listSubnets(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig);
      List<Filter> filters = new ArrayList<>();
      if (isNotEmpty(vpcIds)) {
        filters.add(new Filter("vpc-id", vpcIds));
      }
      filters.add(new Filter("state").withValues("available"));
      tracker.trackEC2Call("List Subnets");
      return amazonEC2Client.describeSubnets(new DescribeSubnetsRequest().withFilters(filters))
          .getSubnets()
          .stream()
          .map(subnet
              -> AwsSubnet.builder()
                     .id(subnet.getSubnetId())
                     .name(CollectionUtils.emptyIfNull(subnet.getTags())
                               .stream()
                               .filter(tag -> NAME.equals(tag.getKey()))
                               .findFirst()
                               .orElse(new Tag(NAME, ""))
                               .getValue())
                     .build())
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<AwsSecurityGroup> listSGs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    List<AwsSecurityGroup> result = new ArrayList<>();
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      String nextToken = null;
      do {
        AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig);
        List<Filter> filters = new ArrayList<>();
        if (isNotEmpty(vpcIds)) {
          filters.add(new Filter("vpc-id", vpcIds));
        }
        tracker.trackEC2Call("List SGs");
        DescribeSecurityGroupsResult describeSecurityGroupsResult = amazonEC2Client.describeSecurityGroups(
            new DescribeSecurityGroupsRequest().withNextToken(nextToken).withFilters(filters));
        List<SecurityGroup> securityGroups = describeSecurityGroupsResult.getSecurityGroups();
        result.addAll(securityGroups.stream()
                          .map(securityGroup
                              -> AwsSecurityGroup.builder()
                                     .id(securityGroup.getGroupId())
                                     .name(securityGroup.getGroupName())
                                     .build())
                          .collect(toList()));
        nextToken = describeSecurityGroupsResult.getNextToken();
      } while (nextToken != null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
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
        AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig);
        tracker.trackEC2Call("List Tags");
        DescribeTagsResult describeTagsResult =
            amazonEC2Client.describeTags(new DescribeTagsRequest()
                                             .withNextToken(nextToken)
                                             .withFilters(new Filter("resource-type").withValues(resourceType))
                                             .withMaxResults(1000));
        tags.addAll(describeTagsResult.getTags().stream().map(TagDescription::getKey).collect(toSet()));
        nextToken = describeTagsResult.getNextToken();
      } while (nextToken != null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
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
        tracker.trackEC2Call("List Ec2 instances");
        DescribeInstancesResult describeInstancesResult =
            getAmazonEc2Client(region, awsConfig).describeInstances(describeInstancesRequest);
        result.addAll(getInstanceList(describeInstancesResult));
        nextToken = describeInstancesResult.getNextToken();
      } while (nextToken != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
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
        tracker.trackEC2Call("List Ec2 instances");
        DescribeInstancesResult describeInstancesResult =
            getAmazonEc2Client(region, awsConfig).describeInstances(describeInstancesRequest);
        result.addAll(getInstanceList(describeInstancesResult));
        nextToken = describeInstancesResult.getNextToken();
      } while (nextToken != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public Set<String> listBlockDeviceNamesOfAmi(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String amiId) {
    try {
      if (isEmpty(amiId)) {
        return emptySet();
      }
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig);
      DescribeImagesRequest request = new DescribeImagesRequest().withImageIds(amiId);
      tracker.trackEC2Call("List Images");
      DescribeImagesResult result = amazonEC2Client.describeImages(request);
      List<Image> images = result.getImages();
      if (isNotEmpty(images)) {
        Optional<Image> optionalImage = images.stream().filter(image -> amiId.equals(image.getImageId())).findFirst();
        if (optionalImage.isPresent()) {
          List<BlockDeviceMapping> blockDeviceMappings = optionalImage.get().getBlockDeviceMappings();
          if (isNotEmpty(blockDeviceMappings)) {
            return blockDeviceMappings.stream().map(BlockDeviceMapping::getDeviceName).collect(toSet());
          }
        }
      }
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptySet();
  }

  @Override
  public LaunchTemplateVersion getLaunchTemplateVersion(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String launchTemplateId, String version) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      final AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig);
      tracker.trackEC2Call("Get Launch Template Version");
      final DescribeLaunchTemplateVersionsResult describeLaunchTemplateVersionsResult =
          amazonEc2Client.describeLaunchTemplateVersions(
              new DescribeLaunchTemplateVersionsRequest().withLaunchTemplateId(launchTemplateId).withVersions(version));
      if (describeLaunchTemplateVersionsResult != null
          && isNotEmpty(describeLaunchTemplateVersionsResult.getLaunchTemplateVersions())) {
        return describeLaunchTemplateVersionsResult.getLaunchTemplateVersions().get(0);
      }

    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }

    return null;
  }

  @Override
  public CreateLaunchTemplateVersionResult createLaunchTemplateVersion(
      CreateLaunchTemplateVersionRequest createLaunchTemplateVersionRequest, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      final AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig);
      tracker.trackEC2Call("Create Launch Template Version");
      return amazonEc2Client.createLaunchTemplateVersion(createLaunchTemplateVersionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }

    return null;
  }

  private List<Instance> getInstanceList(DescribeInstancesResult result) {
    List<Instance> instanceList = Lists.newArrayList();
    result.getReservations().forEach(reservation -> instanceList.addAll(reservation.getInstances()));
    return instanceList;
  }
}