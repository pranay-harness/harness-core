package software.wings.cloudprovider.aws;

import static java.util.Arrays.asList;
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
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;
import software.wings.service.impl.AwsHelperService;

import java.util.Collections;
import java.util.List;

/**
 * Created by anubhaw on 6/22/17.
 */
@Ignore
public class AwsCodeDeployServiceTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(AwsCodeDeployServiceTest.class);
  @InjectMocks @Inject private AwsCodeDeployService awsCodeDeployService;
  @Inject private ScmSecret scmSecret;
  @Mock private AwsHelperService awsHelperService;

  String PUBLIC_DNS_NAME = "publicDnsName";
  SettingAttribute cloudProvider;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    cloudProvider =
        SettingAttribute.Builder.aSettingAttribute()
            .withValue(AwsConfig.builder()
                           .accessKey(scmSecret.decrypt(new SecretName("aws_playground_access_key")).toString())
                           .secretKey(scmSecret.decryptToCharArray(new SecretName("aws_playground_secret_key")))
                           .build())
            .build();
  }

  @Test
  public void shouldListApplication() {
    awsCodeDeployService.listApplications(Regions.US_EAST_1.getName(), cloudProvider, Collections.emptyList())
        .forEach(application -> { logger.info(application.toString()); });

    awsCodeDeployService
        .listDeploymentGroup(Regions.US_EAST_1.getName(), "todolistwar", cloudProvider, Collections.emptyList())
        .forEach(dg -> { logger.info(dg.toString()); });

    awsCodeDeployService
        .listDeploymentConfiguration(Regions.US_EAST_1.getName(), cloudProvider, Collections.emptyList())
        .forEach(dc -> { logger.info(dc.toString()); });

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
            createDeploymentRequest, new ExecutionLogCallback(), 10);
    logger.info(codeDeployDeploymentInfo.toString());
  }

  @Test
  public void shouldListApplicationRevisions() {
    logger.info(awsCodeDeployService
                    .getApplicationRevisionList(Regions.US_EAST_1.getName(), "todolistwar", "todolistwarDG",
                        cloudProvider, Collections.emptyList())
                    .toString());
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
    result.withReservations(asList(new Reservation().withInstances(new Instance().withPublicDnsName(PUBLIC_DNS_NAME))));
    doReturn(result).when(awsHelperService).describeEc2Instances(any(), any(), any(), any());

    instanceList =
        awsCodeDeployService.listDeploymentInstances(Regions.US_EAST_1.getName(), null, null, "deploymentId");
    assertNotNull(instanceList);
    assertEquals(1, instanceList.size());
    assertEquals(PUBLIC_DNS_NAME, instanceList.iterator().next().getPublicDnsName());
  }
}
