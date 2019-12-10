package software.wings.service.impl.aws.delegate;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionRequest;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeLaunchTemplateVersionsRequest;
import com.amazonaws.services.ec2.model.DescribeLaunchTemplateVersionsResult;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.LaunchTemplateVersion;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ec2.model.Vpc;
import io.harness.aws.AwsCallTracker;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.Set;

public class AwsEc2HelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsCallTracker mockTracker;
  @Mock private AmazonEC2Client mockClient;

  @Spy @InjectMocks private AwsEc2HelperServiceDelegateImpl awsEc2HelperServiceDelegate;
  public static final String REGION = "us-east-1";

  @Before
  public void setup() {
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyListOf(EncryptedDataDetail.class));
    doReturn(mockClient).when(awsEc2HelperServiceDelegate).getAmazonEc2Client(anyString(), any());
    doNothing().when(mockTracker).trackEC2Call(anyString());
  }
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testValidateAwsAccountCredential() {
    AmazonEC2Client mockClient = mock(AmazonEC2Client.class);
    doReturn(mockClient).when(awsEc2HelperServiceDelegate).getAmazonEc2Client(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeRegionsResult()).when(mockClient).describeRegions();
    doNothing().when(mockTracker).trackEC2Call(anyString());
    boolean valid = awsEc2HelperServiceDelegate.validateAwsAccountCredential(AwsConfig.builder().build(), emptyList());
    assertThat(valid).isTrue();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListRegions() {
    AmazonEC2Client mockClient = mock(AmazonEC2Client.class);
    doReturn(mockClient).when(awsEc2HelperServiceDelegate).getAmazonEc2Client(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeRegionsResult().withRegions(
                 new Region().withRegionName("us-east-1"), new Region().withRegionName("us-east-2")))
        .when(mockClient)
        .describeRegions();
    doNothing().when(mockTracker).trackEC2Call(anyString());
    List<String> regions = awsEc2HelperServiceDelegate.listRegions(AwsConfig.builder().build(), emptyList());
    assertThat(regions).isNotNull();
    assertThat(regions.size()).isEqualTo(2);
    assertThat(regions.get(0)).isEqualTo("us-east-1");
    assertThat(regions.get(1)).isEqualTo("us-east-2");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListVPCs() {
    AmazonEC2Client mockClient = mock(AmazonEC2Client.class);
    doReturn(mockClient).when(awsEc2HelperServiceDelegate).getAmazonEc2Client(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeVpcsResult().withVpcs(new Vpc().withVpcId("vp1"), new Vpc().withVpcId("vp2")))
        .when(mockClient)
        .describeVpcs(any());
    doNothing().when(mockTracker).trackEC2Call(anyString());
    List<String> vpcs = awsEc2HelperServiceDelegate.listVPCs(AwsConfig.builder().build(), emptyList(), "us-east-1");
    assertThat(vpcs).isNotNull();
    assertThat(vpcs.size()).isEqualTo(2);
    assertThat(vpcs.get(0)).isEqualTo("vp1");
    assertThat(vpcs.get(1)).isEqualTo("vp2");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListSubnets() {
    AmazonEC2Client mockClient = mock(AmazonEC2Client.class);
    doReturn(mockClient).when(awsEc2HelperServiceDelegate).getAmazonEc2Client(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeSubnetsResult().withSubnets(new Subnet().withSubnetId("s1"), new Subnet().withSubnetId("s2")))
        .when(mockClient)
        .describeSubnets(any());
    doNothing().when(mockTracker).trackEC2Call(anyString());
    List<String> subnets =
        awsEc2HelperServiceDelegate.listSubnets(AwsConfig.builder().build(), emptyList(), "us-east-1", emptyList());
    assertThat(subnets).isNotNull();
    assertThat(subnets.size()).isEqualTo(2);
    assertThat(subnets.get(0)).isEqualTo("s1");
    assertThat(subnets.get(1)).isEqualTo("s2");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListSGs() {
    AmazonEC2Client mockClient = mock(AmazonEC2Client.class);
    doReturn(mockClient).when(awsEc2HelperServiceDelegate).getAmazonEc2Client(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeSecurityGroupsResult().withSecurityGroups(
                 new SecurityGroup().withGroupId("g1"), new SecurityGroup().withGroupId("g2")))
        .when(mockClient)
        .describeSecurityGroups(any());
    doNothing().when(mockTracker).trackEC2Call(anyString());
    List<String> groups =
        awsEc2HelperServiceDelegate.listSGs(AwsConfig.builder().build(), emptyList(), "us-east-1", emptyList());
    assertThat(groups).isNotNull();
    assertThat(groups.size()).isEqualTo(2);
    assertThat(groups.get(0)).isEqualTo("g1");
    assertThat(groups.get(1)).isEqualTo("g2");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListTags() {
    AmazonEC2Client mockClient = mock(AmazonEC2Client.class);
    doReturn(mockClient).when(awsEc2HelperServiceDelegate).getAmazonEc2Client(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeTagsResult().withTags(new TagDescription().withKey("k1"), new TagDescription().withKey("k2")))
        .when(mockClient)
        .describeTags(any());
    doNothing().when(mockTracker).trackEC2Call(anyString());
    Set<String> tags =
        awsEc2HelperServiceDelegate.listTags(AwsConfig.builder().build(), emptyList(), "us-east-1", "instance");
    assertThat(tags).isNotNull();
    assertThat(tags.size()).isEqualTo(2);
    assertThat(tags.contains("k1")).isTrue();
    assertThat(tags.contains("k2")).isTrue();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListEc2Instances_1() {
    AmazonEC2Client mockClient = mock(AmazonEC2Client.class);
    doReturn(mockClient).when(awsEc2HelperServiceDelegate).getAmazonEc2Client(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeInstancesResult().withReservations(new Reservation().withInstances(
                 new Instance().withInstanceId("id1"), new Instance().withInstanceId("id2"))))
        .when(mockClient)
        .describeInstances(any());
    doNothing().when(mockTracker).trackEC2Call(anyString());
    List<Instance> instances = awsEc2HelperServiceDelegate.listEc2Instances(
        AwsConfig.builder().build(), emptyList(), "us-east-1", emptyList());
    assertThat(instances).isNotNull();
    assertThat(instances.size()).isEqualTo(2);
    assertThat(instances.get(0).getInstanceId()).isEqualTo("id1");
    assertThat(instances.get(1).getInstanceId()).isEqualTo("id2");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListEc2Instances_2() {
    AmazonEC2Client mockClient = mock(AmazonEC2Client.class);
    doReturn(mockClient).when(awsEc2HelperServiceDelegate).getAmazonEc2Client(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeInstancesResult().withReservations(new Reservation().withInstances(
                 new Instance().withInstanceId("id1"), new Instance().withInstanceId("id2"))))
        .when(mockClient)
        .describeInstances(any());
    doNothing().when(mockTracker).trackEC2Call(anyString());
    List<Instance> instances = awsEc2HelperServiceDelegate.listEc2Instances(
        AwsConfig.builder().build(), emptyList(), singletonList("id1"), "us-east-1");
    assertThat(instances).isNotNull();
    assertThat(instances.size()).isEqualTo(2);
    assertThat(instances.get(0).getInstanceId()).isEqualTo("id1");
    assertThat(instances.get(1).getInstanceId()).isEqualTo("id2");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListBlockDeviceNamesOfAmi() {
    AmazonEC2Client mockClient = mock(AmazonEC2Client.class);
    doReturn(mockClient).when(awsEc2HelperServiceDelegate).getAmazonEc2Client(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeImagesResult().withImages(
                 new Image()
                     .withBlockDeviceMappings(new BlockDeviceMapping().withDeviceName("name0"),
                         new BlockDeviceMapping().withDeviceName("name1"))
                     .withImageId("ami")))
        .when(mockClient)
        .describeImages(any());
    doNothing().when(mockTracker).trackEC2Call(anyString());
    Set<String> names =
        awsEc2HelperServiceDelegate.listBlockDeviceNamesOfAmi(AwsConfig.builder().build(), emptyList(), REGION, "ami");
    assertThat(names).isNotNull();
    assertThat(names.size()).isEqualTo(2);
    assertThat(names.contains("name0")).isTrue();
    assertThat(names.contains("name1")).isTrue();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getLaunchTemplateVersion() {
    final LaunchTemplateVersion launchTemplateVersion1 = new LaunchTemplateVersion();
    doReturn(new DescribeLaunchTemplateVersionsResult().withLaunchTemplateVersions(launchTemplateVersion1))
        .when(mockClient)
        .describeLaunchTemplateVersions(any(DescribeLaunchTemplateVersionsRequest.class));
    final LaunchTemplateVersion launchTemplateVersion = awsEc2HelperServiceDelegate.getLaunchTemplateVersion(
        AwsConfig.builder().build(), emptyList(), REGION, "ltid", "3");
    assertThat(launchTemplateVersion).isEqualTo(launchTemplateVersion1);
    verify(mockClient, times(1)).describeLaunchTemplateVersions(any(DescribeLaunchTemplateVersionsRequest.class));
    doThrow(new AmazonClientException("exception"))
        .when(mockClient)
        .describeLaunchTemplateVersions(any(DescribeLaunchTemplateVersionsRequest.class));
    assertThatThrownBy(()
                           -> awsEc2HelperServiceDelegate.getLaunchTemplateVersion(
                               AwsConfig.builder().build(), emptyList(), REGION, "ltid", "3"))
        .isInstanceOf(WingsException.class);

    doThrow(new AmazonServiceException("exception"))
        .when(mockClient)
        .describeLaunchTemplateVersions(any(DescribeLaunchTemplateVersionsRequest.class));
    assertThatThrownBy(()
                           -> awsEc2HelperServiceDelegate.getLaunchTemplateVersion(
                               AwsConfig.builder().build(), emptyList(), REGION, "ltid", "3"))
        .isInstanceOf(WingsException.class);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_createLaunchTemplateVersion() {
    final CreateLaunchTemplateVersionRequest createLaunchTemplateVersionRequest =
        new CreateLaunchTemplateVersionRequest();
    doReturn(new CreateLaunchTemplateVersionResult())
        .when(mockClient)
        .createLaunchTemplateVersion(createLaunchTemplateVersionRequest);

    final CreateLaunchTemplateVersionResult launchTemplateVersionResult =
        awsEc2HelperServiceDelegate.createLaunchTemplateVersion(
            createLaunchTemplateVersionRequest, AwsConfig.builder().build(), emptyList(), REGION);
    verify(mockClient, times(1)).createLaunchTemplateVersion(createLaunchTemplateVersionRequest);
    doThrow(new AmazonServiceException("exception"))
        .when(mockClient)
        .createLaunchTemplateVersion(createLaunchTemplateVersionRequest);
    assertThatThrownBy(()
                           -> awsEc2HelperServiceDelegate.createLaunchTemplateVersion(
                               createLaunchTemplateVersionRequest, AwsConfig.builder().build(), emptyList(), REGION))
        .isInstanceOf(WingsException.class);

    doThrow(new AmazonClientException("exception"))
        .when(mockClient)
        .createLaunchTemplateVersion(createLaunchTemplateVersionRequest);
    assertThatThrownBy(()
                           -> awsEc2HelperServiceDelegate.createLaunchTemplateVersion(
                               createLaunchTemplateVersionRequest, AwsConfig.builder().build(), emptyList(), REGION))
        .isInstanceOf(WingsException.class);
  }
}