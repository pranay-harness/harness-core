package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_INSTANCE_FILTER;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import com.mongodb.DBCursor;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.BlueprintProperty;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.service.impl.InfrastructureProvisionerServiceImpl;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.aws.manager.AwsCFHelperServiceManager;
import software.wings.sm.ExecutionContext;

import java.util.ArrayList;
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
  @Mock FeatureFlagService featureFlagService;
  @Mock AwsCFHelperServiceManager awsCFHelperServiceManager;
  @Inject @InjectMocks InfrastructureProvisionerService infrastructureProvisionerService;

  @Test
  @Category(UnitTests.class)
  public void testRegenerateInfrastructureMappings() throws Exception {
    doReturn(false).when(featureFlagService).isEnabled(eq(FeatureName.INFRA_MAPPING_REFACTOR), any());
    InfrastructureProvisioner infrastructureProvisioner =
        CloudFormationInfrastructureProvisioner.builder()
            .appId(APP_ID)
            .uuid(ID_KEY)
            .mappingBlueprints(Arrays.asList(
                InfrastructureMappingBlueprint.builder()
                    .cloudProviderType(CloudProviderType.AWS)
                    .serviceId(SERVICE_ID)
                    .deploymentType(DeploymentType.SSH)
                    .properties(Arrays.asList(BlueprintProperty.builder()
                                                  .name("region")
                                                  .value("${cloudformation"
                                                      + ".myregion}")
                                                  .build(),
                        BlueprintProperty.builder().name("vpcs").value("${cloudformation.myvpcs}").build(),
                        BlueprintProperty.builder().name("tags").value("${cloudformation.mytags}").build()))
                    .nodeFilteringType(AWS_INSTANCE_FILTER)
                    .build()))
            .build();
    doReturn(infrastructureProvisioner)
        .when(wingsPersistence)
        .getWithAppId(eq(InfrastructureProvisioner.class), anyString(), anyString());
    doReturn(query).when(wingsPersistence).createQuery(eq(InfrastructureMapping.class));
    doReturn(query).doReturn(query).when(query).filter(anyString(), any());
    doReturn(infrastructureMappings).when(query).fetch();
    doReturn(new HashMap<>()).when(executionContext).asMap();

    doReturn(true).doReturn(true).doReturn(false).when(infrastructureMappings).hasNext();
    InfrastructureMapping infrastructureMapping = anAwsInfrastructureMapping()
                                                      .withAppId(APP_ID)
                                                      .withProvisionerId(ID_KEY)
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
    objectMap.put("mytags", "name:mockName");
    CloudFormationCommandResponse commandResponse = CloudFormationCreateStackResponse.builder()
                                                        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                        .output(StringUtils.EMPTY)
                                                        .stackId("11")
                                                        .cloudFormationOutputMap(objectMap)
                                                        .build();

    doReturn(infrastructureMapping).when(infrastructureMappingService).update(any());
    infrastructureProvisionerService.regenerateInfrastructureMappings(ID_KEY, executionContext, objectMap);

    ArgumentCaptor<InfrastructureMapping> captor = ArgumentCaptor.forClass(InfrastructureMapping.class);
    verify(infrastructureMappingService).update(captor.capture());
    InfrastructureMapping mapping = captor.getValue();
    AwsInstanceFilter awsInstanceFilter = ((AwsInfrastructureMapping) mapping).getAwsInstanceFilter();
    assertThat(awsInstanceFilter).isNotNull();
    assertEquals("us-east-1", ((AwsInfrastructureMapping) mapping).getRegion());

    assertThat(awsInstanceFilter.getVpcIds()).isNotNull();
    assertThat(awsInstanceFilter.getVpcIds()).hasSize(3);
    assertThat(awsInstanceFilter.getVpcIds().contains("vpc1")).isTrue();
    assertThat(awsInstanceFilter.getVpcIds().contains("vpc2")).isTrue();
    assertThat(awsInstanceFilter.getVpcIds().contains("vpc3")).isTrue();

    assertThat(awsInstanceFilter.getTags()).isNotNull();
    assertThat(awsInstanceFilter.getTags()).hasSize(1);
    assertEquals("name", awsInstanceFilter.getTags().get(0).getKey());
    assertEquals("mockName", awsInstanceFilter.getTags().get(0).getValue());
  }

  @Test
  @Category(UnitTests.class)
  public void testResolveBlueprints() {
    Map<String, Object> contextMap = new HashMap<>();
    Map<String, Object> blueprints = new HashMap<>();
    InfrastructureProvisionerServiceImpl infrastructureProvisionerService =
        (InfrastructureProvisionerServiceImpl) this.infrastructureProvisionerService;
    assertThatThrownBy(() -> infrastructureProvisionerService.resolveBlueprints(contextMap, blueprints, ""))
        .isInstanceOf(InvalidRequestException.class);
    blueprints.put("abc", "out1");
    assertThatThrownBy(() -> infrastructureProvisionerService.resolveBlueprints(contextMap, blueprints, ""))
        .isInstanceOf(InvalidRequestException.class);
    contextMap.put("out1", 1);
    Map<String, Object> resolveBlueprints =
        infrastructureProvisionerService.resolveBlueprints(contextMap, blueprints, "");
    assertThat(resolveBlueprints.size() == 1).isTrue();
    Object value = resolveBlueprints.get("abc");
    assertThat(value instanceof Integer && (Integer) value == 1).isTrue();

    Map<String, Object> someMap = new HashMap<String, Object>() {
      {
        put("innerpojo", new HashMap<String, Object>() {
          { put("key1", "out2"); }
        });
      }
    };
    blueprints.put("awsInstance", someMap);
    contextMap.put("out2", 2);
    resolveBlueprints = infrastructureProvisionerService.resolveBlueprints(contextMap, blueprints, "");
    value = ((Map) ((Map) resolveBlueprints.get("awsInstance")).get("innerpojo")).get("key1");
    assertThat(value instanceof Integer && (Integer) value == 2).isTrue();

    blueprints.put("key2", new ArrayList<>());
    try {
      infrastructureProvisionerService.resolveBlueprints(contextMap, blueprints, "");
      fail("Should throw exception");
    } catch (InvalidRequestException ex) {
      assertThat(ExceptionUtils.getMessage(ex).contains("Unknown Blueprint value")).isTrue();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCFTemplateParamKeys() {
    String defaultString = "default";

    doReturn(Arrays.asList())
        .when(awsCFHelperServiceManager)
        .getParamsData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

    infrastructureProvisionerService.getCFTemplateParamKeys("GIT", defaultString, defaultString, defaultString,
        defaultString, defaultString, defaultString, defaultString, defaultString, true);
    assertThatThrownBy(
        ()
            -> infrastructureProvisionerService.getCFTemplateParamKeys("GIT", defaultString, defaultString,
                defaultString, defaultString, "", defaultString, defaultString, defaultString, true))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(()
                           -> infrastructureProvisionerService.getCFTemplateParamKeys("GIT", defaultString,
                               defaultString, defaultString, defaultString, defaultString, "", defaultString, "", true))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(
        ()
            -> infrastructureProvisionerService.getCFTemplateParamKeys("TEMPLATE_BODY", defaultString, defaultString,
                "", defaultString, defaultString, defaultString, defaultString, defaultString, true))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(
        ()
            -> infrastructureProvisionerService.getCFTemplateParamKeys("TEMPLATE_URL", defaultString, defaultString, "",
                defaultString, defaultString, defaultString, defaultString, defaultString, true))
        .isInstanceOf(InvalidRequestException.class);
  }
}
