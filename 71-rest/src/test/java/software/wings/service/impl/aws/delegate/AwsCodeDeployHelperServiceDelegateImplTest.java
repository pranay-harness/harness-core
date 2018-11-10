package software.wings.service.impl.aws.delegate;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;

import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codedeploy.model.DeploymentGroupInfo;
import com.amazonaws.services.codedeploy.model.GetDeploymentGroupResult;
import com.amazonaws.services.codedeploy.model.ListApplicationsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesResult;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
import com.amazonaws.services.codedeploy.model.S3Location;
import com.amazonaws.services.ec2.model.Instance;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.aws.model.AwsCodeDeployS3LocationData;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

public class AwsCodeDeployHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsEc2HelperServiceDelegate mockAwsEc2HelperServiceDelegate;
  @Mock private AwsUtils mockAwsUtils;
  @Spy @InjectMocks private AwsCodeDeployHelperServiceDelegateImpl awsCodeDeployHelperServiceDelegate;

  @Test
  public void testListApplications() {
    AmazonCodeDeployClient mockClient = mock(AmazonCodeDeployClient.class);
    doReturn(mockClient)
        .when(awsCodeDeployHelperServiceDelegate)
        .getAmazonCodeDeployClient(any(), anyString(), any(), anyBoolean());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new ListApplicationsResult().withApplications("app1", "app2")).when(mockClient).listApplications(any());
    List<String> applications =
        awsCodeDeployHelperServiceDelegate.listApplications(AwsConfig.builder().build(), emptyList(), "us-east-1");
    assertThat(applications).isNotNull();
    assertThat(applications.size()).isEqualTo(2);
    assertThat(applications.get(0)).isEqualTo("app1");
    assertThat(applications.get(1)).isEqualTo("app2");
  }

  @Test
  public void testListDeploymentConfiguration() {
    AmazonCodeDeployClient mockClient = mock(AmazonCodeDeployClient.class);
    doReturn(mockClient)
        .when(awsCodeDeployHelperServiceDelegate)
        .getAmazonCodeDeployClient(any(), anyString(), any(), anyBoolean());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new ListDeploymentConfigsResult().withDeploymentConfigsList("c1", "c2"))
        .when(mockClient)
        .listDeploymentConfigs(any());
    List<String> configs = awsCodeDeployHelperServiceDelegate.listDeploymentConfiguration(
        AwsConfig.builder().build(), emptyList(), "us-east-1");
    assertThat(configs).isNotNull();
    assertThat(configs.size()).isEqualTo(2);
    assertThat(configs.get(0)).isEqualTo("c1");
    assertThat(configs.get(1)).isEqualTo("c2");
  }

  @Test
  public void testListDeploymentGroups() {
    AmazonCodeDeployClient mockClient = mock(AmazonCodeDeployClient.class);
    doReturn(mockClient)
        .when(awsCodeDeployHelperServiceDelegate)
        .getAmazonCodeDeployClient(any(), anyString(), any(), anyBoolean());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new ListDeploymentGroupsResult().withDeploymentGroups("g1", "g2"))
        .when(mockClient)
        .listDeploymentGroups(any());
    List<String> groups = awsCodeDeployHelperServiceDelegate.listDeploymentGroups(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "app");
    assertThat(groups).isNotNull();
    assertThat(groups.size()).isEqualTo(2);
    assertThat(groups.get(0)).isEqualTo("g1");
    assertThat(groups.get(1)).isEqualTo("g2");
  }

  @Test
  public void testlListDeploymentInstances() {
    AmazonCodeDeployClient mockClient = mock(AmazonCodeDeployClient.class);
    doReturn(mockClient)
        .when(awsCodeDeployHelperServiceDelegate)
        .getAmazonCodeDeployClient(any(), anyString(), any(), anyBoolean());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new ListDeploymentInstancesResult().withInstancesList("i1", "i2"))
        .when(mockClient)
        .listDeploymentInstances(any());
    doReturn(Lists.newArrayList(new Instance().withInstanceId("i1"), new Instance().withInstanceId("13")))
        .when(mockAwsEc2HelperServiceDelegate)
        .listEc2Instances(any(), anyList(), anyString(), anyList());
    List<Instance> result = awsCodeDeployHelperServiceDelegate.listDeploymentInstances(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "id");
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0).getInstanceId()).isEqualTo("i1");
  }

  @Test
  public void testListAppRevision() {
    AmazonCodeDeployClient mockClient = mock(AmazonCodeDeployClient.class);
    doReturn(mockClient)
        .when(awsCodeDeployHelperServiceDelegate)
        .getAmazonCodeDeployClient(any(), anyString(), any(), anyBoolean());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new GetDeploymentGroupResult().withDeploymentGroupInfo(
                 new DeploymentGroupInfo().withTargetRevision(new RevisionLocation().withS3Location(
                     new S3Location().withBucket("bucket").withKey("key").withBundleType("type")))))
        .when(mockClient)
        .getDeploymentGroup(any());
    AwsCodeDeployS3LocationData s3LocationData = awsCodeDeployHelperServiceDelegate.listAppRevision(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "app", "group");
    assertThat(s3LocationData).isNotNull();
    assertThat(s3LocationData.getBucket()).isEqualTo("bucket");
    assertThat(s3LocationData.getKey()).isEqualTo("key");
    assertThat(s3LocationData.getBundleType()).isEqualTo("type");
  }
}