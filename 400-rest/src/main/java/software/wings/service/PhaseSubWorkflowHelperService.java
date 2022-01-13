/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;

import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.collections.CollectionUtils;

@Singleton
public class PhaseSubWorkflowHelperService {
  @Inject TemplateExpressionProcessor templateExpressionProcessor;
  @Inject ServiceResourceService serviceResourceService;
  @Inject InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject InfrastructureMappingService infrastructureMappingService;
  @Inject FeatureFlagService featureFlagService;

  public Service getService(
      String serviceId, TemplateExpression serviceTemplateExpression, String appId, ExecutionContext context) {
    Service service = null;
    if (serviceTemplateExpression != null) {
      service = templateExpressionProcessor.resolveService(context, appId, serviceTemplateExpression);
    } else if (serviceId != null) {
      service = serviceResourceService.get(appId, serviceId);
      notNullCheck("Service not found, check if it's not deleted", service);
    }
    return service;
  }

  public InfrastructureDefinition getInfraDefinition(
      String infraDefinitionId, TemplateExpression infraDefTemplateExpression, String appId, ExecutionContext context) {
    InfrastructureDefinition infrastructureDefinition = null;
    if (infraDefTemplateExpression != null) {
      infrastructureDefinition =
          templateExpressionProcessor.resolveInfraDefinition(context, appId, infraDefTemplateExpression);
    } else if (infraDefinitionId != null) {
      infrastructureDefinition = infrastructureDefinitionService.get(appId, infraDefinitionId);
      notNullCheck("Infra Definition not found, check if it's not deleted", infrastructureDefinition);
    }
    return infrastructureDefinition;
  }

  public InfrastructureMapping getInfraMapping(
      String infraMappingId, TemplateExpression infraMappingExpression, String appId, ExecutionContext context) {
    InfrastructureMapping infrastructureMapping = null;
    if (infraMappingExpression != null) {
      infrastructureMapping = templateExpressionProcessor.resolveInfraMapping(context, appId, infraMappingExpression);
    } else if (infraMappingId != null) {
      infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
      notNullCheck("Service Infrastructure not found, check if it's not deleted", infrastructureMapping);
    }
    return infrastructureMapping;
  }

  public void validateEnvInfraRelationShip(
      Environment environment, InfrastructureDefinition infraDefinition, InfrastructureMapping infrastructureMapping) {
    if (environment == null) {
      return;
    }
    if (infraDefinition != null) {
      if (!environment.getUuid().equals(infraDefinition.getEnvId())) {
        throw new InvalidRequestException(format("Infra Definition [%s] is not associated with Environment [%s]",
            infraDefinition.getName(), environment.getName()));
      }
    }

    if (infrastructureMapping != null) {
      if (!environment.getUuid().equals(infrastructureMapping.getEnvId())) {
        throw new InvalidRequestException(format("Service Infrastructure [%s] is not associated with Environment [%s]",
            infrastructureMapping.getName(), environment.getName()));
      }
    }
  }

  public void validateServiceInfraMappingRelationShip(Service service, InfrastructureMapping infrastructureMapping) {
    if (service != null && infrastructureMapping != null
        && !service.getUuid().equals(infrastructureMapping.getServiceId())) {
      throw new InvalidRequestException(format("Service [%s] is not associated with Service Infra [%s]",
          service.getName(), infrastructureMapping.getName()));
    }
  }

  public void validateScopedServices(Service service, InfrastructureDefinition infrastructureDefinition) {
    if (service != null && infrastructureDefinition != null
        && CollectionUtils.isNotEmpty(infrastructureDefinition.getScopedToServices())
        && !infrastructureDefinition.getScopedToServices().contains(service.getUuid())) {
      throw new InvalidRequestException(
          format("Service [%s] is not in the list of Scoped Services for Infrastructure Definition [%s]",
              service.getName(), infrastructureDefinition.getName()));
    }
  }

  public void validateEntitiesRelationship(Service service, InfrastructureDefinition infrastructureDefinition,
      InfrastructureMapping infrastructureMapping, Environment env, TemplateExpression serviceTemplateExpression,
      TemplateExpression infraMappingTemplateExpression, String accountId) {
    validateScopedServices(service, infrastructureDefinition);
    validateServiceInfraMappingRelationShip(service, infrastructureMapping);
    validateEnvInfraRelationShip(env, infrastructureDefinition, infrastructureMapping);
  }
}
