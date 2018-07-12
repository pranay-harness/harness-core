package software.wings.service.intfc;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
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
  public void testGetRegions() {
    doReturn(asList("r1", "r2")).when(mockAwsHelperService).listRegions(any(), anyList());
    assertEquals(asList("r1", "r2"),
        service.getRegions(AwsConfig.builder().build(), singletonList(EncryptedDataDetail.builder().build())));
  }

  @Test
  public void testGetVPCs() {
    doReturn(asList("vpc1", "vpc2")).when(mockAwsHelperService).listVPCs(any(), anyList(), anyString());
    assertEquals(asList("vpc1", "vpc2"),
        service.getVPCs(AwsConfig.builder().build(), singletonList(EncryptedDataDetail.builder().build()), "region"));
  }

  @Test
  public void testGetSubnets() {
    doReturn(asList("s1", "s2")).when(mockAwsHelperService).listSubnetIds(any(), anyList(), anyString(), anyList());
    assertEquals(asList("s1", "s2"),
        service.getSubnets(AwsConfig.builder().build(), singletonList(EncryptedDataDetail.builder().build()), "region",
            asList("v1", "v2")));
  }

  @Test
  public void testGetSGs() {
    doReturn(asList("s1", "s2"))
        .when(mockAwsHelperService)
        .listSecurityGroupIds(any(), anyList(), anyString(), anyList());
    assertEquals(asList("s1", "s2"),
        service.getSGs(AwsConfig.builder().build(), singletonList(EncryptedDataDetail.builder().build()), "region",
            asList("v1", "v2")));
  }

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
  public void testDescribeEc2Instances() {
    DescribeInstancesResult result = new DescribeInstancesResult().withReservations(
        new Reservation().withInstances(new Instance().withArchitecture("arch")));
    doReturn(result).when(mockAwsHelperService).describeEc2Instances(any(), anyList(), anyString(), any());
    List<Instance> actual = service.describeEc2Instances(
        AwsConfig.builder().build(), singletonList(EncryptedDataDetail.builder().build()), "region", emptyList());
    assertNotNull(actual);
    assertEquals(actual.size(), 1);
    assertEquals(actual.get(0).getArchitecture(), "arch");
  }

  @Test
  public void testGetIAMInstanceRoles() {
    doReturn(asList("r1", "r2")).when(mockAwsHelperService).listIAMInstanceRoles(any());
    assertEquals(asList("r1", "r2"), service.getIAMInstanceRoles(AwsConfig.builder().build()));
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