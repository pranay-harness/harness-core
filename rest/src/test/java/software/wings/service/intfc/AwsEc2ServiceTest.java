package software.wings.service.intfc;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.google.inject.Inject;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsEc2ServiceImpl;
import software.wings.service.impl.AwsHelperService;

import java.util.List;

public class AwsEc2ServiceTest extends WingsBaseTest {
  @Mock private AwsHelperService mockAwsHelperService;

  // Important: The "AwsEc2Service" is to be used "ONLY" in the Delegate Module, and not in the Wings Module.
  // As a result, we are binding the impl itself rather than intfc for writing this test.
  // !!! Do not change !!!
  @Inject @InjectMocks private AwsEc2ServiceImpl service;

  @Test
  public void testDescribeAutoScalingGroupInstances() {
    DescribeInstancesResult result = new DescribeInstancesResult().withReservations(
        new Reservation().withInstances(new Instance().withArchitecture("arch")));
    doReturn(result)
        .when(mockAwsHelperService)
        .describeAutoScalingGroupInstances(any(), anyList(), anyString(), anyString());
    List<Instance> actual = service.describeAutoScalingGroupInstances(
        AwsConfig.builder().build(), singletonList(EncryptedDataDetail.builder().build()), "region", "name");
    assertNotNull(actual);
    assertEquals(actual.size(), 1);
    assertEquals(actual.get(0).getArchitecture(), "arch");
  }

  @Test
  public void testGetAutoScalingGroups() {
    List<AutoScalingGroup> scalingGroups =
        singletonList(new AutoScalingGroup().withAutoScalingGroupName("name").withAutoScalingGroupARN("arn"));
    doReturn(scalingGroups).when(mockAwsHelperService).listAutoScalingGroups(any(), anyList(), anyString());
    List<String> actual = service.getAutoScalingGroupNames(
        AwsConfig.builder().build(), singletonList(EncryptedDataDetail.builder().build()), "region");
    assertNotNull(actual);
    assertEquals(actual.size(), 1);
    assertEquals(actual.get(0), "name");
  }
}