package io.harness.batch.processing.cloudevents.aws.ecs.service.support.impl;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableSet;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.beans.AwsCrossAccountAttributes;

import java.util.List;
import java.util.Set;

public class AwsEC2HelperServiceImplTest extends CategoryTest {
  @Spy private AwsEC2HelperServiceImpl awsEC2HelperService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testListEc2Instances() {
    String nextToken = "nextClusterToken";
    AmazonEC2Client mockClient = mock(AmazonEC2Client.class);
    doReturn(mockClient).when(awsEC2HelperService).getAmazonEC2Client(any(), any());
    Set<String> instanceIds = ImmutableSet.of("instance-1", "instance-2");
    Instance instanceOne = new Instance().withInstanceId("instance-1");
    Instance instanceTwo = new Instance().withInstanceId("instance-2");
    doReturn(new DescribeInstancesResult()
                 .withReservations(new Reservation().withInstances(instanceOne))
                 .withNextToken(nextToken))
        .when(mockClient)
        .describeInstances(new DescribeInstancesRequest().withNextToken(null).withInstanceIds(instanceIds));

    doReturn(new DescribeInstancesResult()
                 .withReservations(new Reservation().withInstances(instanceTwo))
                 .withNextToken(null))
        .when(mockClient)
        .describeInstances(new DescribeInstancesRequest().withNextToken(nextToken).withInstanceIds(instanceIds));

    List<Instance> instances =
        awsEC2HelperService.listEc2Instances(AwsCrossAccountAttributes.builder().build(), instanceIds, "REGION");
    assertThat(instances).hasSize(2);
    assertThat(instances.get(0)).isEqualTo(instanceOne);
  }
}
