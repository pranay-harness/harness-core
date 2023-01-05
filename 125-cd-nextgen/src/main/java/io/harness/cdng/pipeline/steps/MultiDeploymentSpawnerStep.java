/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.pipeline.steps;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.steps.SdkCoreStepUtils.createStepResponseFromChildResponse;

import io.harness.beans.FeatureName;
import io.harness.cdng.envgroup.yaml.EnvironmentGroupYaml;
import io.harness.cdng.environment.filters.FilterType;
import io.harness.cdng.environment.filters.FilterYaml;
import io.harness.cdng.environment.filters.TagsFilter;
import io.harness.cdng.environment.helper.EnvironmentInfraFilterHelper;
import io.harness.cdng.environment.yaml.EnvironmentYamlV2;
import io.harness.cdng.environment.yaml.EnvironmentsMetadata;
import io.harness.cdng.environment.yaml.EnvironmentsYaml;
import io.harness.cdng.environment.yaml.ServiceOverrideInputsYaml;
import io.harness.cdng.infra.yaml.InfraStructureDefinitionYaml;
import io.harness.cdng.pipeline.beans.MultiDeploymentStepParameters;
import io.harness.cdng.pipeline.steps.EnvironmentMapResponse.EnvironmentMapResponseBuilder;
import io.harness.cdng.service.beans.ServiceYamlV2;
import io.harness.cdng.service.beans.ServicesMetadata;
import io.harness.cdng.service.beans.ServicesYaml;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.contracts.execution.MatrixMetadata;
import io.harness.pms.contracts.execution.StrategyMetadata;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.executable.ChildrenExecutableWithRollbackAndRbac;
import io.harness.tasks.ResponseData;
import io.harness.utils.NGFeatureFlagHelperService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MultiDeploymentSpawnerStep extends ChildrenExecutableWithRollbackAndRbac<MultiDeploymentStepParameters> {
  @Inject private NGFeatureFlagHelperService featureFlagHelperService;
  @Inject private EnvironmentInfraFilterHelper environmentInfraFilterHelper;
  @Inject private ServiceEntityService serviceEntityService;

  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType("multiDeployment").setStepCategory(StepCategory.STRATEGY).build();

  @Override
  public StepResponse handleChildrenResponseInternal(
      Ambiance ambiance, MultiDeploymentStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Completed  execution for MultiDeploymentSpawner Step [{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  @Override
  public Class<MultiDeploymentStepParameters> getStepParametersClass() {
    return MultiDeploymentStepParameters.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, MultiDeploymentStepParameters stepParameters) {
    // Do Nothing
  }

  @Override
  public ChildrenExecutableResponse obtainChildrenAfterRbac(
      Ambiance ambiance, MultiDeploymentStepParameters stepParameters, StepInputPackage inputPackage) {
    List<ChildrenExecutableResponse.Child> children = new ArrayList<>();
    List<Map<String, String>> servicesMap = getServicesMap(stepParameters.getServices());

    List<EnvironmentMapResponse> environmentsMapList = new ArrayList<>();

    String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);

    String childNodeId = stepParameters.getChildNodeId();

    // Separate handling as parallelism works differently when filters are present with service.tags expression
    if (featureFlagHelperService.isEnabled(accountIdentifier, FeatureName.CDS_FILTER_INFRA_CLUSTERS_ON_TAGS)
        && (environmentInfraFilterHelper.isServiceTagsExpressionPresent(stepParameters.getEnvironments())
            || environmentInfraFilterHelper.isServiceTagsExpressionPresent(stepParameters.getEnvironmentGroup()))) {
      return getChildrenExecutableResponse(
          stepParameters, children, accountIdentifier, orgIdentifier, projectIdentifier, childNodeId);
    }

    environmentInfraFilterHelper.processEnvInfraFiltering(accountIdentifier, orgIdentifier, projectIdentifier,
        stepParameters.getEnvironments(), stepParameters.getEnvironmentGroup());
    if (stepParameters.getEnvironments() != null) {
      environmentsMapList = getEnvironmentMapList(stepParameters.getEnvironments());
    } else if (stepParameters.getEnvironmentGroup() != null) {
      environmentsMapList = getEnvironmentsGroupMap(stepParameters.getEnvironmentGroup());
    }

    if (servicesMap.isEmpty()) {
      // This case is when the deployment is of type single service multiple environment/infras
      return getChildrenExecutionResponseForMultiEnvironment(
          stepParameters, children, environmentsMapList, childNodeId);
    }

    if (environmentsMapList.isEmpty()) {
      List<ServiceOverrideInputsYaml> servicesOverrides = stepParameters.getServicesOverrides();
      Map<String, Map<String, Object>> serviceRefToOverrides = new HashMap<>();
      if (servicesOverrides != null) {
        serviceRefToOverrides =
            servicesOverrides.stream().collect(Collectors.toMap(ServiceOverrideInputsYaml::getServiceRef,
                overrideInput -> overrideInput.getServiceOverrideInputs().getValue()));
      }
      int currentIteration = 0;
      int totalIterations = servicesMap.size();
      int maxConcurrency = 0;
      if (stepParameters.getServices().getServicesMetadata() != null
          && stepParameters.getServices().getServicesMetadata().getParallel() != null
          && !stepParameters.getServices().getServicesMetadata().getParallel()) {
        maxConcurrency = 1;
      }
      for (Map<String, String> serviceMap : servicesMap) {
        String serviceRef = MultiDeploymentSpawnerUtils.getServiceRef(serviceMap);
        if (serviceRefToOverrides.containsKey(serviceRef)) {
          MultiDeploymentSpawnerUtils.addServiceOverridesToMap(serviceMap, serviceRefToOverrides.get(serviceRef));
        }
        children.add(getChild(childNodeId, currentIteration, totalIterations, serviceMap,
            MultiDeploymentSpawnerUtils.MULTI_SERVICE_DEPLOYMENT));
        currentIteration++;
      }
      return ChildrenExecutableResponse.newBuilder().addAllChildren(children).setMaxConcurrency(maxConcurrency).build();
    }

    boolean isServiceParallel = stepParameters.getServices() != null
        && shouldDeployInParallel(stepParameters.getServices().getServicesMetadata());
    boolean isEnvironmentParallel = stepParameters.getEnvironmentGroup() != null
        || (stepParameters.getEnvironments() != null
            && shouldDeployInParallel(stepParameters.getEnvironments().getEnvironmentsMetadata()));

    int currentIteration = 0;
    int totalIterations = servicesMap.size() * environmentsMapList.size();
    int maxConcurrency = 0;
    if (isServiceParallel) {
      if (!isEnvironmentParallel) {
        maxConcurrency = servicesMap.size();
      } else {
        maxConcurrency = totalIterations;
      }
      for (EnvironmentMapResponse environmentMap : environmentsMapList) {
        for (Map<String, String> serviceMap : servicesMap) {
          children.add(
              getChildForMultiServiceInfra(childNodeId, currentIteration, totalIterations, serviceMap, environmentMap));
          currentIteration++;
        }
      }
    } else if (isEnvironmentParallel) {
      maxConcurrency = environmentsMapList.size();
      for (Map<String, String> serviceMap : servicesMap) {
        for (EnvironmentMapResponse environmentMap : environmentsMapList) {
          children.add(
              getChildForMultiServiceInfra(childNodeId, currentIteration, totalIterations, serviceMap, environmentMap));
          currentIteration++;
        }
      }
    } else {
      for (EnvironmentMapResponse environmentMap : environmentsMapList) {
        for (Map<String, String> serviceMap : servicesMap) {
          children.add(
              getChildForMultiServiceInfra(childNodeId, currentIteration, totalIterations, serviceMap, environmentMap));
          currentIteration++;
        }
      }
    }
    return ChildrenExecutableResponse.newBuilder().addAllChildren(children).setMaxConcurrency(maxConcurrency).build();
  }

  private ChildrenExecutableResponse getChildrenExecutionResponseForMultiEnvironment(
      MultiDeploymentStepParameters stepParameters, List<ChildrenExecutableResponse.Child> children,
      List<EnvironmentMapResponse> environmentsMapList, String childNodeId) {
    int currentIteration = 0;
    int totalIterations = environmentsMapList.size();
    int maxConcurrency = 0;
    if (isEnvironmentSeries(stepParameters)) {
      maxConcurrency = 1;
    }
    for (EnvironmentMapResponse environmentMapResponse : environmentsMapList) {
      Map<String, String> environmentMap = environmentMapResponse.getEnvironmentsMapList();
      if (environmentMapResponse.getServiceOverrideInputsYamlMap() != null
          && environmentMapResponse.getServiceOverrideInputsYamlMap().size() > 1) {
        throw new InvalidYamlException(
            "Found more than one service in overrides for a single service deployment. Please correct the yaml and try");
      }
      if (EmptyPredicate.isNotEmpty(environmentMapResponse.getServiceOverrideInputsYamlMap())) {
        MultiDeploymentSpawnerUtils.addServiceOverridesToMap(environmentMap,
            environmentMapResponse.getServiceOverrideInputsYamlMap()
                .entrySet()
                .iterator()
                .next()
                .getValue()
                .getServiceOverrideInputs()
                .getValue());
      }
      children.add(getChild(childNodeId, currentIteration, totalIterations, environmentMap,
          MultiDeploymentSpawnerUtils.MULTI_ENV_DEPLOYMENT));
      currentIteration++;
    }
    return ChildrenExecutableResponse.newBuilder().addAllChildren(children).setMaxConcurrency(maxConcurrency).build();
  }

  private boolean isEnvironmentSeries(MultiDeploymentStepParameters stepParameters) {
    return stepParameters.getEnvironments() != null
        && stepParameters.getEnvironments().getEnvironmentsMetadata() != null
        && stepParameters.getEnvironments().getEnvironmentsMetadata().getParallel() != null
        && !stepParameters.getEnvironments().getEnvironmentsMetadata().getParallel()
        || stepParameters.getEnvironmentGroup() != null
        && stepParameters.getEnvironmentGroup().getEnvironmentGroupMetadata() != null
        && stepParameters.getEnvironmentGroup().getEnvironmentGroupMetadata().getParallel() != null
        && !stepParameters.getEnvironmentGroup().getEnvironmentGroupMetadata().getParallel();
  }

  private ChildrenExecutableResponse getChildrenExecutableResponse(MultiDeploymentStepParameters stepParameters,
      List<ChildrenExecutableResponse.Child> children, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String childNodeId) {
    Map<String, Map<String, String>> serviceToMatrixMetadataMap =
        getServiceToMatrixMetadataMap(stepParameters.getServices());
    // Find service tags
    Map<String, List<NGTag>> serviceTagsMap =
        getFetchServiceTags(accountIdentifier, orgIdentifier, projectIdentifier, stepParameters);

    // Parse the yaml and set the FilterYaml tag value
    Map<String, EnvironmentsYaml> serviceEnvYamlMap =
        getServiceToEnvsYaml(serviceTagsMap, stepParameters.getEnvironments());

    Map<String, EnvironmentGroupYaml> serviceEnvGroupMap =
        getServiceToEnvGroup(serviceTagsMap, stepParameters.getEnvironmentGroup());

    Map<String, List<EnvironmentMapResponse>> serviceEnvMatrixMap = new LinkedHashMap<>();
    for (String serviceRef : serviceTagsMap.keySet()) {
      EnvironmentsYaml environmentsYaml = serviceEnvYamlMap.get(serviceRef);
      EnvironmentGroupYaml environmentGroupYaml = serviceEnvGroupMap.get(serviceRef);
      environmentInfraFilterHelper.processEnvInfraFiltering(
          accountIdentifier, orgIdentifier, projectIdentifier, environmentsYaml, environmentGroupYaml);
      List<EnvironmentMapResponse> environmentMapList;
      if (environmentsYaml != null) {
        environmentMapList = getEnvironmentMapList(environmentsYaml);
      } else if (environmentGroupYaml != null) {
        environmentMapList = getEnvironmentsGroupMap(environmentGroupYaml);
      } else {
        throw new InvalidRequestException("No environments found for service: " + serviceRef);
      }
      serviceEnvMatrixMap.put(serviceRef, environmentMapList);
    }

    int maxConcurrency = 0;
    // If Both service and env are non-parallel
    if (stepParameters.getServices().getServicesMetadata() != null
        && stepParameters.getServices().getServicesMetadata().getParallel() != null
        && !stepParameters.getServices().getServicesMetadata().getParallel()
        && ((stepParameters.getEnvironments() != null
                && stepParameters.getEnvironments().getEnvironmentsMetadata() != null
                && stepParameters.getEnvironments().getEnvironmentsMetadata().getParallel() != null
                && !stepParameters.getEnvironments().getEnvironmentsMetadata().getParallel())
            || (stepParameters.getEnvironmentGroup() != null
                && stepParameters.getEnvironmentGroup().getEnvironmentGroupMetadata() != null
                && stepParameters.getEnvironmentGroup().getEnvironmentGroupMetadata().getParallel() != null
                && !stepParameters.getEnvironmentGroup().getEnvironmentGroupMetadata().getParallel()))) {
      maxConcurrency = 1;
    }
    int totalIterations = 0;
    for (Map.Entry<String, List<EnvironmentMapResponse>> serviceEnv : serviceEnvMatrixMap.entrySet()) {
      totalIterations += serviceEnv.getValue().size();
    }

    int currentIteration = 0;
    for (Map.Entry<String, List<EnvironmentMapResponse>> serviceEnv : serviceEnvMatrixMap.entrySet()) {
      String serviceRef = serviceEnv.getKey();
      for (EnvironmentMapResponse envMap : serviceEnv.getValue()) {
        Map<String, String> serviceMatrixMetadata = serviceToMatrixMetadataMap.get(serviceRef);
        children.add(getChildForMultiServiceInfra(
            childNodeId, currentIteration, totalIterations, serviceMatrixMetadata, envMap));
      }
    }
    return ChildrenExecutableResponse.newBuilder().addAllChildren(children).setMaxConcurrency(maxConcurrency).build();
  }

  private Map<String, Map<String, String>> getServiceToMatrixMetadataMap(ServicesYaml servicesYaml) {
    if (servicesYaml == null) {
      return new LinkedHashMap<>();
    }
    if (ParameterField.isNull(servicesYaml.getValues())) {
      throw new InvalidYamlException("Expected a value of serviceRefs to be provided but found null");
    }
    if (servicesYaml.getValues().isExpression()) {
      throw new InvalidYamlException("Expression could not be resolved for services yaml");
    }
    List<ServiceYamlV2> services = servicesYaml.getValues().getValue();
    if (services.isEmpty()) {
      throw new InvalidYamlException("No value of services provided. Please provide atleast one value");
    }
    Map<String, Map<String, String>> serviceToMatrixMetadataMap = new LinkedHashMap<>();
    for (ServiceYamlV2 service : services) {
      serviceToMatrixMetadataMap.put(
          service.getServiceRef().getValue(), MultiDeploymentSpawnerUtils.getMapFromServiceYaml(service));
    }
    return serviceToMatrixMetadataMap;
  }

  private Map<String, EnvironmentGroupYaml> getServiceToEnvGroup(
      Map<String, List<NGTag>> serviceTagsMap, EnvironmentGroupYaml environmentGroup) {
    Map<String, EnvironmentGroupYaml> serviceEnvGroupMap = new LinkedHashMap<>();

    if (environmentGroup == null) {
      return serviceEnvGroupMap;
    }
    for (Map.Entry<String, List<NGTag>> serviceTag : serviceTagsMap.entrySet()) {
      EnvironmentGroupYaml envGroupPerService = environmentGroup.clone();

      if (ParameterField.isNotNull(envGroupPerService.getFilters())) {
        ParameterField<List<FilterYaml>> filters = envGroupPerService.getFilters();
        resolveServiceTags(serviceTag, filters);
      }

      ParameterField<List<EnvironmentYamlV2>> environments = envGroupPerService.getEnvironments();
      if (ParameterField.isNotNull(environments) && isNotEmpty(environments.getValue())) {
        resolveServiceTags(serviceTag, environments.getValue());
      }
      serviceEnvGroupMap.put(serviceTag.getKey(), envGroupPerService);
    }

    return serviceEnvGroupMap;
  }

  private Map<String, EnvironmentsYaml> getServiceToEnvsYaml(
      Map<String, List<NGTag>> serviceTagsMap, EnvironmentsYaml environments) {
    Map<String, EnvironmentsYaml> serviceEnvMap = new LinkedHashMap<>();

    if (environments == null) {
      return serviceEnvMap;
    }
    for (Map.Entry<String, List<NGTag>> serviceTag : serviceTagsMap.entrySet()) {
      EnvironmentsYaml environmentsYamlPerService = environments.clone();

      if (ParameterField.isNotNull(environmentsYamlPerService.getFilters())) {
        ParameterField<List<FilterYaml>> filters = environmentsYamlPerService.getFilters();
        resolveServiceTags(serviceTag, filters);
      }

      ParameterField<List<EnvironmentYamlV2>> values = environmentsYamlPerService.getValues();
      if (ParameterField.isNotNull(values) && isNotEmpty(values.getValue())) {
        resolveServiceTags(serviceTag, values.getValue());
      }
      serviceEnvMap.put(serviceTag.getKey(), environmentsYamlPerService);
    }

    return serviceEnvMap;
  }

  private void resolveServiceTags(
      Map.Entry<String, List<NGTag>> serviceTag, List<EnvironmentYamlV2> environmentYamlV2s) {
    if (isEmpty(environmentYamlV2s)) {
      return;
    }
    for (EnvironmentYamlV2 environmentYamlV2 : environmentYamlV2s) {
      ParameterField<List<FilterYaml>> filters = environmentYamlV2.getFilters();
      resolveServiceTags(serviceTag, filters);
    }
  }

  private void resolveServiceTags(Map.Entry<String, List<NGTag>> serviceTag, ParameterField<List<FilterYaml>> filters) {
    if (ParameterField.isNotNull(filters) && isNotEmpty(filters.getValue())) {
      for (FilterYaml filterYaml : filters.getValue()) {
        if (filterYaml.getType().equals(FilterType.tags)) {
          TagsFilter tagsFilter = (TagsFilter) filterYaml.getSpec();
          if (tagsFilter.getTags().isExpression()
              && tagsFilter.getTags().getExpressionValue().contains("<+service.tags>")) {
            tagsFilter.setTags(ParameterField.createValueField(getTagsMap(serviceTag.getValue())));
          }
        }
      }
    }
  }

  public Map<String, String> getTagsMap(List<NGTag> ngTagList) {
    Map<String, String> tags = new LinkedHashMap<>();
    for (NGTag ngTag : ngTagList) {
      tags.put(ngTag.getKey(), ngTag.getValue());
    }
    return tags;
  }
  private Map<String, List<NGTag>> getFetchServiceTags(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, MultiDeploymentStepParameters multiDeploymentStepParameters) {
    ServicesYaml services = multiDeploymentStepParameters.getServices();
    if (services == null || ParameterField.isNull(services.getValues())) {
      throw new InvalidYamlException("Services cannot be null when filters are specified");
    }

    List<ServiceYamlV2> serviceYamlV2List = services.getValues().getValue();
    List<String> serviceRefs =
        serviceYamlV2List.stream().map(s -> s.getServiceRef().getValue()).collect(Collectors.toList());

    if (isEmpty(serviceRefs)) {
      serviceRefs =
          Collections.singletonList(multiDeploymentStepParameters.getServiceYamlV2().getServiceRef().getValue());
    }

    List<ServiceEntity> serviceEntityList =
        serviceEntityService.getServices(accountIdentifier, orgIdentifier, projectIdentifier, serviceRefs);

    Map<String, List<NGTag>> serviceToTagsMap = new LinkedHashMap<>();
    for (ServiceEntity serviceEntity : serviceEntityList) {
      serviceToTagsMap.put(serviceEntity.getIdentifier(), serviceEntity.getTags());
    }
    return serviceToTagsMap;
  }

  private ChildrenExecutableResponse.Child getChild(
      String childNodeId, int currentIteration, int totalIterations, Map<String, String> serviceMap, String subType) {
    return ChildrenExecutableResponse.Child.newBuilder()
        .setChildNodeId(childNodeId)
        .setStrategyMetadata(
            StrategyMetadata.newBuilder()
                .setCurrentIteration(currentIteration)
                .setTotalIterations(totalIterations)
                .setMatrixMetadata(
                    MatrixMetadata.newBuilder().setSubType(subType).putAllMatrixValues(serviceMap).build())
                .build())
        .build();
  }

  private boolean shouldDeployInParallel(EnvironmentsMetadata metadata) {
    return metadata != null && metadata.getParallel() != null && metadata.getParallel();
  }

  private boolean shouldDeployInParallel(ServicesMetadata metadata) {
    return metadata != null && metadata.getParallel() != null && metadata.getParallel();
  }

  private ChildrenExecutableResponse.Child getChildForMultiServiceInfra(String childNodeId, int currentIteration,
      int totalIterations, Map<String, String> serviceMap, EnvironmentMapResponse environmentMapResponse) {
    Map<String, String> matrixMetadataMap = new HashMap<>();
    matrixMetadataMap.putAll(serviceMap);
    Map<String, String> environmentMap = environmentMapResponse.getEnvironmentsMapList();
    String serviceRef = MultiDeploymentSpawnerUtils.getServiceRef(serviceMap);
    if (environmentMapResponse.getServiceOverrideInputsYamlMap() != null
        && environmentMapResponse.getServiceOverrideInputsYamlMap().containsKey(serviceRef)) {
      MultiDeploymentSpawnerUtils.addServiceOverridesToMap(environmentMap,
          environmentMapResponse.getServiceOverrideInputsYamlMap()
              .get(serviceRef)
              .getServiceOverrideInputs()
              .getValue());
    }
    matrixMetadataMap.putAll(environmentMap);
    String subType;
    if (environmentMap.isEmpty()) {
      subType = MultiDeploymentSpawnerUtils.MULTI_SERVICE_DEPLOYMENT;
    } else if (serviceMap.isEmpty()) {
      subType = MultiDeploymentSpawnerUtils.MULTI_ENV_DEPLOYMENT;
    } else {
      subType = MultiDeploymentSpawnerUtils.MULTI_SERVICE_ENV_DEPLOYMENT;
    }
    return getChild(childNodeId, currentIteration, totalIterations, matrixMetadataMap, subType);
  }

  private List<EnvironmentMapResponse> getEnvironmentMapList(EnvironmentsYaml environmentsYaml) {
    if (environmentsYaml == null) {
      return new ArrayList<>();
    }
    if (ParameterField.isNull(environmentsYaml.getValues())) {
      throw new InvalidYamlException("Expected a value of serviceRefs to be provided but found null");
    }
    if (environmentsYaml.getValues().isExpression()) {
      throw new InvalidYamlException("Expression could not be resolved for environments yaml");
    }
    List<EnvironmentYamlV2> environments = environmentsYaml.getValues().getValue();
    return getEnvironmentsMap(environments);
  }

  private List<EnvironmentMapResponse> getEnvironmentsGroupMap(EnvironmentGroupYaml environmentGroupYaml) {
    if (environmentGroupYaml.getEnvironments().isExpression()) {
      throw new InvalidYamlException("Expected a value of environmentRefs to be provided but found expression");
    }
    List<EnvironmentYamlV2> environments = environmentGroupYaml.getEnvironments().getValue();
    if (EmptyPredicate.isEmpty(environments)) {
      throw new InvalidYamlException("Expected a value of environmentRefs to be provided but found empty");
    }

    return getEnvironmentsMap(environments);
  }

  private List<EnvironmentMapResponse> getEnvironmentsMap(List<EnvironmentYamlV2> environments) {
    if (EmptyPredicate.isEmpty(environments)) {
      throw new InvalidYamlException("No value of environment provided. Please provide atleast one value");
    }
    List<EnvironmentMapResponse> environmentMapResponses = new ArrayList<>();
    for (EnvironmentYamlV2 environmentYamlV2 : environments) {
      EnvironmentMapResponseBuilder environmentMapResponseBuilder = EnvironmentMapResponse.builder();
      if (ParameterField.isNull(environmentYamlV2.getInfrastructureDefinitions())) {
        environmentMapResponseBuilder.environmentsMapList(MultiDeploymentSpawnerUtils.getMapFromEnvironmentYaml(
            environmentYamlV2, environmentYamlV2.getInfrastructureDefinition().getValue()));
      } else {
        if (environmentYamlV2.getInfrastructureDefinitions().getValue() == null) {
          throw new InvalidYamlException("No infrastructure definition provided. Please provide atleast one value");
        }
        for (InfraStructureDefinitionYaml infra : environmentYamlV2.getInfrastructureDefinitions().getValue()) {
          environmentMapResponseBuilder.environmentsMapList(
              MultiDeploymentSpawnerUtils.getMapFromEnvironmentYaml(environmentYamlV2, infra));
        }
      }
      if (EmptyPredicate.isNotEmpty(environmentYamlV2.getServicesOverrides())) {
        Map<String, ServiceOverrideInputsYaml> serviceRefToServiceOverrides = new HashMap<>();
        for (ServiceOverrideInputsYaml serviceOverrideInputsYaml : environmentYamlV2.getServicesOverrides()) {
          serviceRefToServiceOverrides.put(serviceOverrideInputsYaml.getServiceRef(), serviceOverrideInputsYaml);
        }
        environmentMapResponseBuilder.serviceOverrideInputsYamlMap(serviceRefToServiceOverrides);
      }
      environmentMapResponses.add(environmentMapResponseBuilder.build());
    }
    return environmentMapResponses;
  }

  private List<Map<String, String>> getServicesMap(ServicesYaml servicesYaml) {
    if (servicesYaml == null) {
      return new ArrayList<>();
    }
    if (ParameterField.isNull(servicesYaml.getValues())) {
      throw new InvalidYamlException("Expected a value of serviceRefs to be provided but found null");
    }
    if (servicesYaml.getValues().isExpression()) {
      throw new InvalidYamlException("Expression could not be resolved for services yaml");
    }
    List<ServiceYamlV2> services = servicesYaml.getValues().getValue();
    if (services.isEmpty()) {
      throw new InvalidYamlException("No value of services provided. Please provide atleast one value");
    }
    List<Map<String, String>> environmentsMap = new ArrayList<>();
    for (ServiceYamlV2 service : services) {
      environmentsMap.add(MultiDeploymentSpawnerUtils.getMapFromServiceYaml(service));
    }
    return environmentsMap;
  }
}
