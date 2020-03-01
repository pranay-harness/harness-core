package software.wings.service.impl;

import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_INSTANCE_FILTER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.PROVISIONER_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.mongodb.DBCursor;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.joor.Reflect;
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
import software.wings.beans.EntityType;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisionerDetails;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.manager.AwsCFHelperServiceManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InfrastructureProvisionerServiceImplTest extends WingsBaseTest {
  @Mock WingsPersistence wingsPersistence;
  @Mock ExecutionContext executionContext;
  @Mock Query query;
  @Mock DBCursor dbCursor;
  @Mock MorphiaIterator infrastructureMappings;
  @Mock InfrastructureMappingService infrastructureMappingService;
  @Mock FeatureFlagService featureFlagService;
  @Mock AwsCFHelperServiceManager awsCFHelperServiceManager;
  @Mock ServiceResourceService serviceResourceService;
  @Mock SettingsService settingService;
  @Mock ResourceLookupService resourceLookupService;
  @Mock AppService appService;
  @Inject @InjectMocks InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject @InjectMocks InfrastructureProvisionerServiceImpl infrastructureProvisionerServiceImpl;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testRegenerateInfrastructureMappings() throws Exception {
    doReturn(false).when(featureFlagService).isEnabled(eq(FeatureName.INFRA_MAPPING_REFACTOR), any());
    InfrastructureProvisioner infrastructureProvisioner =
        CloudFormationInfrastructureProvisioner.builder()
            .appId(APP_ID)
            .uuid(ID_KEY)
            .mappingBlueprints(
                asList(InfrastructureMappingBlueprint.builder()
                           .cloudProviderType(CloudProviderType.AWS)
                           .serviceId(SERVICE_ID)
                           .deploymentType(DeploymentType.SSH)
                           .properties(asList(BlueprintProperty.builder()
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

    PageResponse<Service> response = new PageResponse<>();
    Service service = Service.builder().name("service1").uuid(SERVICE_ID).build();
    response.setResponse(singletonList(service));
    doReturn(response).when(serviceResourceService).list(any(), anyBoolean(), anyBoolean(), anyBoolean(), any());

    infrastructureProvisionerService.regenerateInfrastructureMappings(ID_KEY, executionContext, objectMap);

    ArgumentCaptor<InfrastructureMapping> captor = ArgumentCaptor.forClass(InfrastructureMapping.class);
    verify(infrastructureMappingService).update(captor.capture());
    InfrastructureMapping mapping = captor.getValue();
    AwsInstanceFilter awsInstanceFilter = ((AwsInfrastructureMapping) mapping).getAwsInstanceFilter();
    assertThat(awsInstanceFilter).isNotNull();
    assertThat(((AwsInfrastructureMapping) mapping).getRegion()).isEqualTo("us-east-1");

    assertThat(awsInstanceFilter.getVpcIds()).isNotNull();
    assertThat(awsInstanceFilter.getVpcIds()).hasSize(3);
    assertThat(awsInstanceFilter.getVpcIds().contains("vpc1")).isTrue();
    assertThat(awsInstanceFilter.getVpcIds().contains("vpc2")).isTrue();
    assertThat(awsInstanceFilter.getVpcIds().contains("vpc3")).isTrue();

    assertThat(awsInstanceFilter.getTags()).isNotNull();
    assertThat(awsInstanceFilter.getTags()).hasSize(1);
    assertThat(awsInstanceFilter.getTags().get(0).getKey()).isEqualTo("name");
    assertThat(awsInstanceFilter.getTags().get(0).getValue()).isEqualTo("mockName");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetCFTemplateParamKeys() {
    String defaultString = "default";

    doReturn(asList())
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

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldValidateInfrastructureProvisioner() {
    TerraformInfrastructureProvisioner terraformProvisioner = TerraformInfrastructureProvisioner.builder()
                                                                  .accountId(ACCOUNT_ID)
                                                                  .appId(APP_ID)
                                                                  .name("tf-test")
                                                                  .sourceRepoBranch("master")
                                                                  .path("module/main.tf")
                                                                  .sourceRepoSettingId(SETTING_ID)
                                                                  .build();
    InfrastructureProvisionerServiceImpl provisionerService = spy(InfrastructureProvisionerServiceImpl.class);
    provisionerService.validateProvisioner(terraformProvisioner);

    shouldValidateRepoBranch(terraformProvisioner, provisionerService);
    provisionerService.validateProvisioner(terraformProvisioner);

    shouldValidatePath(terraformProvisioner, provisionerService);
    provisionerService.validateProvisioner(terraformProvisioner);

    shouldValidateSourceRepo(terraformProvisioner, provisionerService);
    provisionerService.validateProvisioner(terraformProvisioner);

    shouldVariablesValidation(terraformProvisioner, provisionerService);
    provisionerService.validateProvisioner(terraformProvisioner);

    shouldBackendConfigValidation(terraformProvisioner, provisionerService);
    provisionerService.validateProvisioner(terraformProvisioner);
  }

  private void shouldBackendConfigValidation(TerraformInfrastructureProvisioner terraformProvisioner,
      InfrastructureProvisionerServiceImpl provisionerService) {
    terraformProvisioner.setBackendConfigs(
        asList(NameValuePair.builder().name("access.key").valueType(Type.TEXT.toString()).build(),
            NameValuePair.builder().name("secret_key").valueType(Type.TEXT.toString()).build()));
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> provisionerService.validateProvisioner(terraformProvisioner));

    terraformProvisioner.setBackendConfigs(
        asList(NameValuePair.builder().name("$access_key").valueType(Type.TEXT.toString()).build(),
            NameValuePair.builder().name("secret_key").valueType(Type.TEXT.toString()).build()));
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> provisionerService.validateProvisioner(terraformProvisioner));

    terraformProvisioner.setBackendConfigs(null);
    provisionerService.validateProvisioner(terraformProvisioner);

    terraformProvisioner.setBackendConfigs(Collections.emptyList());
    provisionerService.validateProvisioner(terraformProvisioner);

    terraformProvisioner.setBackendConfigs(
        asList(NameValuePair.builder().name("access_key").valueType(Type.TEXT.toString()).build(),
            NameValuePair.builder().name("secret_key").valueType(Type.TEXT.toString()).build()));
    provisionerService.validateProvisioner(terraformProvisioner);
  }

  private void shouldVariablesValidation(TerraformInfrastructureProvisioner terraformProvisioner,
      InfrastructureProvisionerServiceImpl provisionerService) {
    terraformProvisioner.setVariables(
        asList(NameValuePair.builder().name("access.key").valueType(Type.TEXT.toString()).build(),
            NameValuePair.builder().name("secret_key").valueType(Type.TEXT.toString()).build()));
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> provisionerService.validateProvisioner(terraformProvisioner));

    terraformProvisioner.setVariables(
        asList(NameValuePair.builder().name("$access_key").valueType(Type.TEXT.toString()).build(),
            NameValuePair.builder().name("secret_key").valueType(Type.TEXT.toString()).build()));
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> provisionerService.validateProvisioner(terraformProvisioner));

    terraformProvisioner.setVariables(null);
    provisionerService.validateProvisioner(terraformProvisioner);

    terraformProvisioner.setVariables(Collections.emptyList());
    provisionerService.validateProvisioner(terraformProvisioner);

    terraformProvisioner.setVariables(
        asList(NameValuePair.builder().name("access_key").valueType(Type.TEXT.toString()).build(),
            NameValuePair.builder().name("secret_key").valueType(Type.TEXT.toString()).build()));
    provisionerService.validateProvisioner(terraformProvisioner);
  }

  private void shouldValidateSourceRepo(TerraformInfrastructureProvisioner terraformProvisioner,
      InfrastructureProvisionerServiceImpl provisionerService) {
    terraformProvisioner.setSourceRepoSettingId("");
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> provisionerService.validateProvisioner(terraformProvisioner));
    terraformProvisioner.setSourceRepoSettingId(null);
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> provisionerService.validateProvisioner(terraformProvisioner));
    terraformProvisioner.setSourceRepoSettingId("settingId");
  }

  private void shouldValidatePath(TerraformInfrastructureProvisioner terraformProvisioner,
      InfrastructureProvisionerServiceImpl provisionerService) {
    terraformProvisioner.setPath(null);
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> provisionerService.validateProvisioner(terraformProvisioner));
    terraformProvisioner.setPath("module/main.tf");
  }

  private void shouldValidateRepoBranch(TerraformInfrastructureProvisioner terraformProvisioner,
      InfrastructureProvisionerServiceImpl provisionerService) {
    terraformProvisioner.setSourceRepoBranch("");
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> provisionerService.validateProvisioner(terraformProvisioner));
    terraformProvisioner.setSourceRepoBranch(null);
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> provisionerService.validateProvisioner(terraformProvisioner));
    terraformProvisioner.setSourceRepoBranch("master");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void nonProvisionerExpressionsResolutionShouldNotFailOnNonResolution() {
    String workflowVariable = "${workflowVariables.var1}";
    Map<String, Object> contextMap = null;
    List<NameValuePair> properties = new ArrayList<>();
    properties.add(NameValuePair.builder().value(workflowVariable).build());
    ManagerExpressionEvaluator evaluator = mock(ManagerExpressionEvaluator.class);
    Reflect.on(infrastructureProvisionerService).set("evaluator", evaluator);
    when(evaluator.evaluate(workflowVariable, contextMap)).thenReturn(null);

    ((InfrastructureProvisionerServiceImpl) infrastructureProvisionerService)
        .getPropertyNameEvaluatedMap(
            properties, contextMap, true, TerraformInfrastructureProvisioner.INFRASTRUCTURE_PROVISIONER_TYPE_KEY);

    verify(evaluator, times(1)).evaluate(workflowVariable, contextMap);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void provisionerExpressionsResolutionShouldFailOnNonResolution() {
    String provisionerVariable = "${terraform.var1}";
    Map<String, Object> contextMap = null;
    List<NameValuePair> properties = new ArrayList<>();
    properties.add(NameValuePair.builder().value(provisionerVariable).build());
    ManagerExpressionEvaluator evaluator = mock(ManagerExpressionEvaluator.class);
    Reflect.on(infrastructureProvisionerService).set("evaluator", evaluator);
    when(evaluator.evaluate(provisionerVariable, contextMap)).thenReturn(null);

    ((InfrastructureProvisionerServiceImpl) infrastructureProvisionerService)
        .getPropertyNameEvaluatedMap(properties, contextMap, true, TerraformInfrastructureProvisioner.VARIABLE_KEY);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetIdToServiceMapping() {
    PageRequest<Service> servicePageRequest = new PageRequest<>();
    servicePageRequest.addFilter(Service.APP_ID_KEY, Operator.EQ, APP_ID);
    Set<String> serviceIds = Sets.newHashSet(asList("id1", "id2"));
    servicePageRequest.addFilter(ServiceKeys.uuid, Operator.IN, serviceIds.toArray());
    PageResponse<Service> services = new PageResponse<>();
    Service service1 = Service.builder().name("service1").uuid("id1").build();
    Service service2 = Service.builder().name("service2").uuid("id2").build();
    services.setResponse(asList(service1, service2));
    when(serviceResourceService.list(servicePageRequest, false, false, false, null)).thenReturn(services);

    Map<String, Service> idToServiceMapping = ((InfrastructureProvisionerServiceImpl) infrastructureProvisionerService)
                                                  .getIdToServiceMapping(APP_ID, serviceIds);

    assertThat(idToServiceMapping).hasSize(2);
    assertThat(idToServiceMapping.get("id1")).isEqualTo(service1);
    assertThat(idToServiceMapping.get("id2")).isEqualTo(service2);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetIdToServiceMappingForEmptyServiceIds() {
    Map<String, Service> idToServiceMapping = ((InfrastructureProvisionerServiceImpl) infrastructureProvisionerService)
                                                  .getIdToServiceMapping(APP_ID, Collections.emptySet());

    assertThat(idToServiceMapping).isEmpty();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetIdToSettingAttributeMapping() {
    PageRequest<SettingAttribute> settingAttributePageRequest = new PageRequest<>();
    settingAttributePageRequest.addFilter(SettingAttribute.ACCOUNT_ID_KEY, Operator.EQ, ACCOUNT_ID);
    settingAttributePageRequest.addFilter(
        SettingAttribute.VALUE_TYPE_KEY, Operator.EQ, SettingVariableTypes.GIT.name());
    Set<String> settingAttributeIds = Sets.newHashSet(asList("id1", "id2"));
    settingAttributePageRequest.addFilter(SettingAttributeKeys.uuid, Operator.IN, settingAttributeIds.toArray());
    PageResponse<SettingAttribute> settingAttributePageResponse = new PageResponse<>();
    SettingAttribute settingAttribute1 = Builder.aSettingAttribute().withUuid("id1").build();
    SettingAttribute settingAttribute2 = Builder.aSettingAttribute().withUuid("id2").build();
    settingAttributePageResponse.setResponse(asList(settingAttribute1, settingAttribute2));
    when(settingService.list(settingAttributePageRequest, null, null)).thenReturn(settingAttributePageResponse);

    Map<String, SettingAttribute> idToSettingAttributeMapping =
        ((InfrastructureProvisionerServiceImpl) infrastructureProvisionerService)
            .getIdToSettingAttributeMapping(ACCOUNT_ID, settingAttributeIds);

    assertThat(idToSettingAttributeMapping).hasSize(2);
    assertThat(idToSettingAttributeMapping.get("id1")).isEqualTo(settingAttribute1);
    assertThat(idToSettingAttributeMapping.get("id2")).isEqualTo(settingAttribute2);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetIdToSettingAttributeMappingForEmptySettingAttributeIds() {
    Map<String, SettingAttribute> idToSettingAttributeMapping =
        ((InfrastructureProvisionerServiceImpl) infrastructureProvisionerService)
            .getIdToSettingAttributeMapping(ACCOUNT_ID, Collections.emptySet());

    assertThat(idToSettingAttributeMapping).isEmpty();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldListDetails() {
    InfrastructureProvisionerServiceImpl ipService = spy(new InfrastructureProvisionerServiceImpl());
    Reflect.on(ipService).set("resourceLookupService", resourceLookupService);
    Reflect.on(ipService).set("appService", appService);
    PageRequest<InfrastructureProvisioner> infraProvisionerPageRequest = new PageRequest<>();
    PageResponse<InfrastructureProvisioner> infraProvisionerPageResponse = new PageResponse<>();
    TerraformInfrastructureProvisioner provisioner =
        TerraformInfrastructureProvisioner.builder()
            .sourceRepoSettingId("settingId")
            .mappingBlueprints(singletonList(InfrastructureMappingBlueprint.builder().serviceId("serviceId").build()))
            .build();
    infraProvisionerPageResponse.setResponse(singletonList(provisioner));
    doReturn(infraProvisionerPageResponse)
        .when(resourceLookupService)
        .listWithTagFilters(infraProvisionerPageRequest, null, EntityType.PROVISIONER, true);
    doReturn(ACCOUNT_ID).when(appService).getAccountIdByAppId(APP_ID);
    HashSet<String> settingAttributeIds = new HashSet<>(singletonList("settingId"));
    Map<String, SettingAttribute> idToSettingAttributeMapping = new HashMap<>();
    doReturn(idToSettingAttributeMapping)
        .when(ipService)
        .getIdToSettingAttributeMapping(ACCOUNT_ID, settingAttributeIds);
    HashSet<String> servicesIds = new HashSet<>(singletonList("serviceId"));
    Map<String, Service> idToServiceMapping = new HashMap<>();
    doReturn(idToServiceMapping).when(ipService).getIdToServiceMapping(APP_ID, servicesIds);
    InfrastructureProvisionerDetails infrastructureProvisionerDetails =
        InfrastructureProvisionerDetails.builder().build();
    doReturn(infrastructureProvisionerDetails)
        .when(ipService)
        .details(provisioner, idToSettingAttributeMapping, idToServiceMapping);

    PageResponse<InfrastructureProvisionerDetails> infraProvisionerDetailsPageResponse =
        ipService.listDetails(infraProvisionerPageRequest, true, null, APP_ID);

    assertThat(infraProvisionerDetailsPageResponse.getResponse()).hasSize(1);
    assertThat(infraProvisionerDetailsPageResponse.getResponse().get(0)).isEqualTo(infrastructureProvisionerDetails);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldListDetailsForEmptyInfraMappingBlueprints() {
    InfrastructureProvisionerServiceImpl ipService = spy(new InfrastructureProvisionerServiceImpl());
    Reflect.on(ipService).set("resourceLookupService", resourceLookupService);
    Reflect.on(ipService).set("appService", appService);
    PageRequest<InfrastructureProvisioner> infraProvisionerPageRequest = new PageRequest<>();
    PageResponse<InfrastructureProvisioner> infraProvisionerPageResponse = new PageResponse<>();
    TerraformInfrastructureProvisioner provisioner =
        TerraformInfrastructureProvisioner.builder().sourceRepoSettingId("settingId").build();
    infraProvisionerPageResponse.setResponse(singletonList(provisioner));
    doReturn(infraProvisionerPageResponse)
        .when(resourceLookupService)
        .listWithTagFilters(infraProvisionerPageRequest, null, EntityType.PROVISIONER, true);
    doReturn(ACCOUNT_ID).when(appService).getAccountIdByAppId(APP_ID);
    HashSet<String> settingAttributeIds = new HashSet<>(singletonList("settingId"));
    Map<String, SettingAttribute> idToSettingAttributeMapping = new HashMap<>();
    doReturn(idToSettingAttributeMapping)
        .when(ipService)
        .getIdToSettingAttributeMapping(ACCOUNT_ID, settingAttributeIds);
    HashSet<String> servicesIds = new HashSet<>();
    Map<String, Service> idToServiceMapping = new HashMap<>();
    doReturn(idToServiceMapping).when(ipService).getIdToServiceMapping(APP_ID, servicesIds);
    InfrastructureProvisionerDetails infrastructureProvisionerDetails =
        InfrastructureProvisionerDetails.builder().build();
    doReturn(infrastructureProvisionerDetails)
        .when(ipService)
        .details(provisioner, idToSettingAttributeMapping, idToServiceMapping);

    PageResponse<InfrastructureProvisionerDetails> infraProvisionerDetailsPageResponse =
        ipService.listDetails(infraProvisionerPageRequest, true, null, APP_ID);

    assertThat(infraProvisionerDetailsPageResponse.getResponse()).hasSize(1);
    assertThat(infraProvisionerDetailsPageResponse.getResponse().get(0)).isEqualTo(infrastructureProvisionerDetails);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void shouldFilterDeletedServices() {
    InfrastructureProvisionerServiceImpl ipService = spy(new InfrastructureProvisionerServiceImpl());
    WingsPersistence mockWingsPersistence = mock(WingsPersistence.class);
    Reflect.on(ipService).set("wingsPersistence", mockWingsPersistence);
    FeatureFlagService mockFeatureFlagService = mock(FeatureFlagService.class);
    Reflect.on(ipService).set("featureFlagService", mockFeatureFlagService);
    String SVC_ID_00 = "svc-00";
    String SVC_ID_01 = "svc-01";
    doReturn(false).when(mockFeatureFlagService).isEnabled(eq(FeatureName.INFRA_MAPPING_REFACTOR), any());
    InfrastructureProvisioner provisioner =
        TerraformInfrastructureProvisioner.builder()
            .mappingBlueprints(asList(InfrastructureMappingBlueprint.builder().serviceId(SVC_ID_00).build(),
                InfrastructureMappingBlueprint.builder().serviceId(SVC_ID_01).build()))
            .build();
    doReturn(provisioner).when(mockWingsPersistence).getWithAppId(any(), anyString(), anyString());
    Map<String, Service> map = new HashMap<>();
    map.put("svc-00", Service.builder().uuid(SVC_ID_00).name("name-00").build());
    doReturn(map).when(ipService).getIdToServiceMapping(anyString(), anySet());
    InfrastructureProvisioner returned = ipService.get(APP_ID, PROVISIONER_ID);
    assertThat(returned).isNotNull();
    assertThat(returned.getMappingBlueprints().size()).isEqualTo(1);
    assertThat(returned.getMappingBlueprints().get(0).getServiceId()).isEqualTo(SVC_ID_00);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testRemoveDuplicateVariables() {
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder().build();
    infrastructureProvisionerServiceImpl.removeDuplicateVariables(provisioner);

    provisioner.setVariables(emptyList());
    infrastructureProvisionerServiceImpl.removeDuplicateVariables(provisioner);

    NameValuePair var1 = NameValuePair.builder().name("var1").build();
    NameValuePair var2 = NameValuePair.builder().name("var2").build();
    NameValuePair duplicateVar1 = NameValuePair.builder().name("var1").build();

    provisioner.setVariables(Arrays.asList(var1, var2, duplicateVar1));
    infrastructureProvisionerServiceImpl.removeDuplicateVariables(provisioner);
    assertThat(provisioner.getVariables()).hasSize(2);
    assertThat(provisioner.getVariables().get(0)).isEqualTo(var1);
    assertThat(provisioner.getVariables().get(1)).isEqualTo(var2);
  }
}