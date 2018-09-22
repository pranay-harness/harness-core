package software.wings.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_INSTANCE_FILTER;
import static software.wings.common.Constants.UUID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import com.mongodb.DBCursor;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.sm.ExecutionContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class InfrastructureProvisionerServiceImplTest extends WingsBaseTest {
  @Mock WingsPersistence wingsPersistence;
  @Mock ExecutionContext executionContext;
  @Mock Query query;
  @Mock DBCursor dbCursor;
  @Mock MorphiaIterator infrastructureMappings;
  @Mock InfrastructureMappingService infrastructureMappingService;
  @Inject @InjectMocks InfrastructureProvisionerService infrastructureProvisionerService;

  @Test
  public void testRegenerateInfrastructureMappings() throws Exception {
    InfrastructureProvisioner infrastructureProvisioner =
        CloudFormationInfrastructureProvisioner.builder()
            .appId(APP_ID)
            .uuid(UUID)
            .mappingBlueprints(Arrays.asList(
                InfrastructureMappingBlueprint.builder()
                    .cloudProviderType(CloudProviderType.AWS)
                    .serviceId(SERVICE_ID)
                    .deploymentType(DeploymentType.SSH)
                    .properties(Arrays.asList(
                        NameValuePair.builder().name("region").value("${cloudformation.myregion}").build(),
                        NameValuePair.builder().name("vpcs").value("${cloudformation.myvpcs}").build(),
                        NameValuePair.builder().name("subnets").value("${cloudformation.mysubnets}").build(),
                        NameValuePair.builder()
                            .name("securityGroups")
                            .value("${cloudformation.mysecuritygroups}")
                            .build(),
                        NameValuePair.builder().name("tags").value("${cloudformation.mytags}").build()))
                    .nodeFilteringType(AWS_INSTANCE_FILTER)
                    .build()))
            .build();
    doReturn(infrastructureProvisioner)
        .when(wingsPersistence)
        .get(eq(InfrastructureProvisioner.class), anyString(), anyString());
    doReturn(query).when(wingsPersistence).createQuery(eq(InfrastructureMapping.class));
    doReturn(query).doReturn(query).when(query).filter(anyString(), any());
    doReturn(infrastructureMappings).when(query).fetch();
    doReturn(new HashMap<>()).when(executionContext).asMap();

    doReturn(true).doReturn(false).when(infrastructureMappings).hasNext();
    InfrastructureMapping infrastructureMapping = anAwsInfrastructureMapping()
                                                      .withAppId(APP_ID)
                                                      .withProvisionerId(UUID)
                                                      .withServiceId(SERVICE_ID)
                                                      .withInfraMappingType(InfrastructureMappingType.AWS_SSH.name())
                                                      .build();

    doReturn(infrastructureMapping).when(infrastructureMappings).next();
    doReturn(dbCursor).when(infrastructureMappings).getCursor();

    Map<String, Object> tagMap = new HashMap<>();
    tagMap.put("name", "mockName");
    Map<String, Object> objectMap = new HashMap<>();
    objectMap.put("myregion", "us-east-1");
    objectMap.put("myvpcs", "vpc1,vpc2,vpc3");
    objectMap.put("mysubnets", "subnet1,subnet2,subnet3");
    objectMap.put("mysecuritygroups", "sg1,sg2,sg3");
    objectMap.put("mytags", "name:mockName");
    CloudFormationCommandResponse commandResponse = CloudFormationCreateStackResponse.builder()
                                                        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                        .output(StringUtils.EMPTY)
                                                        .stackId("11")
                                                        .cloudFormationOutputMap(objectMap)
                                                        .build();

    doReturn(infrastructureMapping).when(infrastructureMappingService).update(any());
    infrastructureProvisionerService.regenerateInfrastructureMappings(UUID, executionContext, objectMap);

    ArgumentCaptor<InfrastructureMapping> captor = ArgumentCaptor.forClass(InfrastructureMapping.class);
    verify(infrastructureMappingService).update(captor.capture());
    InfrastructureMapping mapping = captor.getValue();
    AwsInstanceFilter awsInstanceFilter = ((AwsInfrastructureMapping) mapping).getAwsInstanceFilter();
    assertNotNull(awsInstanceFilter);
    assertEquals("us-east-1", ((AwsInfrastructureMapping) mapping).getRegion());

    assertNotNull(awsInstanceFilter.getVpcIds());
    assertEquals(3, awsInstanceFilter.getVpcIds().size());
    assertTrue(awsInstanceFilter.getVpcIds().contains("vpc1"));
    assertTrue(awsInstanceFilter.getVpcIds().contains("vpc2"));
    assertTrue(awsInstanceFilter.getVpcIds().contains("vpc3"));

    assertNotNull(awsInstanceFilter.getSubnetIds());
    assertEquals(3, awsInstanceFilter.getSubnetIds().size());
    assertTrue(awsInstanceFilter.getSubnetIds().contains("subnet1"));
    assertTrue(awsInstanceFilter.getSubnetIds().contains("subnet2"));
    assertTrue(awsInstanceFilter.getSubnetIds().contains("subnet3"));

    assertNotNull(awsInstanceFilter.getSecurityGroupIds());
    assertEquals(3, awsInstanceFilter.getSecurityGroupIds().size());
    assertTrue(awsInstanceFilter.getSecurityGroupIds().contains("sg1"));
    assertTrue(awsInstanceFilter.getSecurityGroupIds().contains("sg2"));
    assertTrue(awsInstanceFilter.getSecurityGroupIds().contains("sg3"));

    assertNotNull(awsInstanceFilter.getTags());
    assertEquals(1, awsInstanceFilter.getTags().size());
    assertEquals("name", awsInstanceFilter.getTags().get(0).getKey());
    assertEquals("mockName", awsInstanceFilter.getTags().get(0).getValue());
  }

  // We could have a similar test for terraform, We can write the test below later.
  // Commenting out this one for now
  /*
  @Test
  public void testRegenerateInfrastructureMappings_terraform() throws Exception {
    InfrastructureProvisioner infrastructureProvisioner =
        TerraformInfrastructureProvisioner.builder()
            .appId(APP_ID)
            .uuid(UUID)
            .mappingBlueprints(Arrays.asList(
                InfrastructureMappingBlueprint.builder()
                    .cloudProviderType(CloudProviderType.AWS)
                    .serviceId(SERVICE_ID)
                    .deploymentType(DeploymentType.SSH)
                    .properties(Arrays.asList(
                        NameValuePair.builder().name("region").value("${terraform.myregion}").build(),
                        NameValuePair.builder().name("vpcs").value("${terraform.myvpcs}").build(),
                        NameValuePair.builder().name("subnets").value("${terraform.mysubnets}").build(),
                        NameValuePair.builder().name("securityGroups").value("${terraform.mysecuritygroups}").build(),
                        NameValuePair.builder().name("tags").value("${terraform.mytags}").build()))
                    .build()))
            .build();
    // Query query = mock(Query.class);
    doReturn(infrastructureProvisioner)
        .when(wingsPersistence)
        .get(eq(InfrastructureProvisioner.class), anyString(), anyString());
    doReturn(query).when(wingsPersistence).createQuery(eq(InfrastructureMapping.class));
    doReturn(query).doReturn(query).when(query).filter(anyString(), any());
    doReturn(infrastructureMappings).when(query).fetch();
    doReturn(new HashMap<>()).when(executionContext).asMap();
    // DBCursor dBCursor = Mockito.mock(com.mongodb.DBCursor.class);

    doReturn(true).doReturn(false).when(infrastructureMappings).hasNext();
    InfrastructureMapping infrastructureMapping = anAwsInfrastructureMapping()
                                                      .withAppId(APP_ID)
                                                      .withProvisionerId(UUID)
                                                      .withServiceId(SERVICE_ID)
                                                      .withInfraMappingType(InfrastructureMappingType.AWS_SSH.name())
                                                      .build();

    doReturn(infrastructureMapping).when(infrastructureMappings).next();
    doReturn(dbCursor).when(infrastructureMappings).getCursor();

    Map<String, Object> tagMap = new HashMap<>();
    tagMap.put("name", "mockName");
    Map<String, Object> objectMap = new HashMap<>();
    objectMap.put("myregion", "us-east-1");
    objectMap.put("myvpcs", Arrays.asList("vpc1", "vpc2", "vpc3"));
    objectMap.put("mysubnets", Arrays.asList("subnet1", "subnet2", "subnet3"));
    objectMap.put("mysecuritygroups", Arrays.asList("sg1", "sg2", "sg3"));
    objectMap.put("mytags", tagMap);
    CloudFormationCommandResponse commandResponse = CloudFormationCreateStackResponse.builder()
                                                        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                        .output(StringUtils.EMPTY)
                                                        .stackId("11")
                                                        .cloudFormationOutputMap(objectMap)
                                                        .build();

    doReturn(infrastructureMapping).when(infrastructureMappingService).update(any());
    infrastructureProvisionerService.regenerateInfrastructureMappings(UUID, executionContext, objectMap);

    ArgumentCaptor<InfrastructureMapping> captor = ArgumentCaptor.forClass(InfrastructureMapping.class);
    verify(infrastructureMappingService).update(captor.capture());
    InfrastructureMapping mapping = captor.getValue();
    AwsInstanceFilter awsInstanceFilter = ((AwsInfrastructureMapping) mapping).getAwsInstanceFilter();
    assertNotNull(awsInstanceFilter);
    assertEquals("us-east-1", ((AwsInfrastructureMapping) mapping).getRegion());

    assertNotNull(awsInstanceFilter.getVpcIds());
    assertEquals(3, awsInstanceFilter.getVpcIds().size());
    assertTrue(awsInstanceFilter.getVpcIds().contains("vpc1"));
    assertTrue(awsInstanceFilter.getVpcIds().contains("vpc2"));
    assertTrue(awsInstanceFilter.getVpcIds().contains("vpc3"));

    assertNotNull(awsInstanceFilter.getSubnetIds());
    assertEquals(3, awsInstanceFilter.getSubnetIds().size());
    assertTrue(awsInstanceFilter.getSubnetIds().contains("subnet1"));
    assertTrue(awsInstanceFilter.getSubnetIds().contains("subnet2"));
    assertTrue(awsInstanceFilter.getSubnetIds().contains("subnet3"));

    assertNotNull(awsInstanceFilter.getSecurityGroupIds());
    assertEquals(3, awsInstanceFilter.getSecurityGroupIds().size());
    assertTrue(awsInstanceFilter.getSecurityGroupIds().contains("sg1"));
    assertTrue(awsInstanceFilter.getSecurityGroupIds().contains("sg2"));
    assertTrue(awsInstanceFilter.getSecurityGroupIds().contains("sg3"));

    assertNotNull(awsInstanceFilter.getTags());
    assertEquals(1, awsInstanceFilter.getTags().size());
    assertEquals("name", awsInstanceFilter.getTags().get(0).getKey());
    assertEquals("mockName", awsInstanceFilter.getTags().get(0).getValue());
  }
  */
}
