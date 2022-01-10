/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.cloudprovider.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANUBHAW;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;
import software.wings.service.impl.AwsHelperService;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesResult;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
import com.amazonaws.services.codedeploy.model.S3Location;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Created by anubhaw on 6/22/17.
 */
@Slf4j
@OwnedBy(CDP)
public class AwsCodeDeployServiceTest extends WingsBaseTest {
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
                           .accessKey(scmSecret.decryptToCharArray(new SecretName("aws_playground_access_key")))
                           .secretKey(scmSecret.decryptToCharArray(new SecretName("aws_playground_secret_key")))
                           .build())
            .build();
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldListApplication() {
    awsCodeDeployService.listApplications(Regions.US_EAST_1.getName(), cloudProvider, Collections.emptyList())
        .forEach(application -> { log.info(application.toString()); });

    awsCodeDeployService
        .listDeploymentGroup(Regions.US_EAST_1.getName(), "todolistwar", cloudProvider, Collections.emptyList())
        .forEach(dg -> { log.info(dg.toString()); });

    awsCodeDeployService
        .listDeploymentConfiguration(Regions.US_EAST_1.getName(), cloudProvider, Collections.emptyList())
        .forEach(dc -> { log.info(dc.toString()); });

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
    log.info(codeDeployDeploymentInfo.toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldListApplicationRevisions() {
    log.info(awsCodeDeployService
                 .getApplicationRevisionList(Regions.US_EAST_1.getName(), "todolistwar", "todolistwarDG", cloudProvider,
                     Collections.emptyList())
                 .toString());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldListDeploymentInstances() {
    doReturn(AwsConfig.builder().build()).when(awsHelperService).validateAndGetAwsConfig(any(), any(), false);

    ListDeploymentInstancesResult listDeploymentInstancesResult = new ListDeploymentInstancesResult();
    listDeploymentInstancesResult.setInstancesList(Collections.EMPTY_LIST);
    listDeploymentInstancesResult.setNextToken(null);
    doReturn(listDeploymentInstancesResult).when(awsHelperService).listDeploymentInstances(any(), any(), any(), any());

    List<Instance> instanceList =
        awsCodeDeployService.listDeploymentInstances(Regions.US_EAST_1.getName(), null, null, "deploymentId");
    assertThat(instanceList).isNotNull();
    assertThat(instanceList).isEmpty();

    listDeploymentInstancesResult.setInstancesList(Collections.singletonList("Ec2InstanceId"));
    doReturn(listDeploymentInstancesResult).when(awsHelperService).listDeploymentInstances(any(), any(), any(), any());

    DescribeInstancesRequest request = new DescribeInstancesRequest();
    doReturn(request).when(awsHelperService).getDescribeInstancesRequestWithRunningFilter();

    DescribeInstancesResult result = new DescribeInstancesResult();
    result.withReservations(asList(new Reservation().withInstances(new Instance().withPublicDnsName(PUBLIC_DNS_NAME))));
    doReturn(result).when(awsHelperService).describeEc2Instances(any(), any(), any(), any());

    instanceList =
        awsCodeDeployService.listDeploymentInstances(Regions.US_EAST_1.getName(), null, null, "deploymentId");
    assertThat(instanceList).isNotNull();
    assertThat(instanceList).hasSize(1);
    assertThat(instanceList.iterator().next().getPublicDnsName()).isEqualTo(PUBLIC_DNS_NAME);
  }
}
