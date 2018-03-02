package software.wings.cloudprovider.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesResult;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
import com.amazonaws.services.codedeploy.model.S3Location;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;
import software.wings.service.impl.AwsHelperService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by anubhaw on 6/22/17.
 */
@Ignore
public class AwsCodeDeployServiceTest extends WingsBaseTest {
  @InjectMocks @Inject private AwsCodeDeployService awsCodeDeployService;
  @Mock private AwsHelperService awsHelperService;

  String PUBLIC_DNS_NAME = "publicDnsName";

  SettingAttribute cloudProvider =
      SettingAttribute.Builder.aSettingAttribute()
          .withValue(AwsConfig.builder()
                         .accessKey("AKIAJLEKM45P4PO5QUFQ")
                         .secretKey("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".toCharArray())
                         .build())
          .build();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void shouldListApplication() {
    awsCodeDeployService.listApplications(Regions.US_EAST_1.getName(), cloudProvider, Collections.emptyList())
        .forEach(application -> { System.out.println(application.toString()); });

    awsCodeDeployService
        .listDeploymentGroup(Regions.US_EAST_1.getName(), "todolistwar", cloudProvider, Collections.emptyList())
        .forEach(dg -> { System.out.println(dg.toString()); });

    awsCodeDeployService
        .listDeploymentConfiguration(Regions.US_EAST_1.getName(), cloudProvider, Collections.emptyList())
        .forEach(dc -> { System.out.println(dc.toString()); });

    CreateDeploymentRequest createDeploymentRequest =
        new CreateDeploymentRequest()
            .withApplicationName("todolistwar")
            .withDeploymentGroupName("todolistwarDG")
            .withDeploymentConfigName("CodeDeployDefault.OneAtATime")
            .withRevision(new RevisionLocation().withRevisionType("S3").withS3Location(
                new S3Location()
                    .withBucket("harnessapps")
                    .withBundleType("zip")
                    .withKey("todolist_war/19/codedeploysample.zip")));
    CodeDeployDeploymentInfo codeDeployDeploymentInfo =
        awsCodeDeployService.deployApplication(Regions.US_EAST_1.getName(), cloudProvider, Collections.emptyList(),
            createDeploymentRequest, new ExecutionLogCallback());
    System.out.println(codeDeployDeploymentInfo);
  }

  @Test
  public void shouldListApplicationRevisions() {
    System.out.println(awsCodeDeployService.getApplicationRevisionList(
        Regions.US_EAST_1.getName(), "todolistwar", "todolistwarDG", cloudProvider, Collections.emptyList()));
  }

  @Test
  public void shouldListDeploymentInstances() {
    doReturn(AwsConfig.builder().build()).when(awsHelperService).validateAndGetAwsConfig(any(), any());

    ListDeploymentInstancesResult listDeploymentInstancesResult = new ListDeploymentInstancesResult();
    listDeploymentInstancesResult.setInstancesList(Collections.EMPTY_LIST);
    listDeploymentInstancesResult.setNextToken(null);
    doReturn(listDeploymentInstancesResult).when(awsHelperService).listDeploymentInstances(any(), any(), any(), any());

    List<Instance> instanceList =
        awsCodeDeployService.listDeploymentInstances(Regions.US_EAST_1.getName(), null, null, "deploymentId");
    assertNotNull(instanceList);
    assertEquals(0, instanceList.size());

    listDeploymentInstancesResult.setInstancesList(Collections.singletonList("Ec2InstanceId"));
    doReturn(listDeploymentInstancesResult).when(awsHelperService).listDeploymentInstances(any(), any(), any(), any());

    DescribeInstancesRequest request = new DescribeInstancesRequest();
    doReturn(request).when(awsHelperService).getDescribeInstancesRequestWithRunningFilter();

    DescribeInstancesResult result = new DescribeInstancesResult();
    result.withReservations(
        Arrays.asList(new Reservation().withInstances(new Instance().withPublicDnsName(PUBLIC_DNS_NAME))));
    doReturn(result).when(awsHelperService).describeEc2Instances(any(), any(), any(), any());

    instanceList =
        awsCodeDeployService.listDeploymentInstances(Regions.US_EAST_1.getName(), null, null, "deploymentId");
    assertNotNull(instanceList);
    assertEquals(1, instanceList.size());
    assertEquals(PUBLIC_DNS_NAME, instanceList.iterator().next().getPublicDnsName());
  }
}
