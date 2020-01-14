package software.wings.graphql.datafetcher.userGroup;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.graphql.schema.mutation.userGroup.QLSetUserGroupPermissionsParameters;
import software.wings.graphql.schema.type.permissions.QLActions;
import software.wings.graphql.schema.type.permissions.QLAppFilter;
import software.wings.graphql.schema.type.permissions.QLAppPermissions;
import software.wings.graphql.schema.type.permissions.QLPermissionType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Class to validate that the input given by the user is correct.
 * All the NULL checks should be done at this layer, so that we can assume that the input passed
 * to the UserController is correct
 * */
@Slf4j
public class UserGroupPermissionValidator {
  @Inject AppService appService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject EnvironmentService environmentService;
  @Inject InfrastructureProvisionerService infrastructureProvisionerService;

  private void checkForInvalidIds(List<String> idsInput, List<String> idsPresent) {
    idsInput.removeAll(idsPresent);
    if (isNotEmpty(idsInput)) {
      throw new InvalidRequestException(
          String.format("Invalid id/s %s provided in the request", String.join(", ", idsInput)));
    }
  }
  private void checkApplicationsExists(Set<String> appIds) {
    if (isEmpty(appIds)) {
      return;
    }
    List<String> ids = new ArrayList<>(appIds);
    PageRequest<Application> req =
        aPageRequest().addFieldsIncluded("_id").addFilter("_id", IN, appIds.toArray()).build();
    PageResponse<Application> res = appService.list(req);
    // This Ids are wrong
    List<String> idsPresent = res.stream().map(Application::getUuid).collect(Collectors.toList());
    checkForInvalidIds(ids, idsPresent);
  }

  private void checkServiceExists(Set<String> serviceIds) {
    if (isEmpty(serviceIds)) {
      return;
    }
    List<String> ids = new ArrayList<>(serviceIds);
    PageRequest<Service> req =
        aPageRequest().addFieldsIncluded("_id").addFilter("_id", IN, serviceIds.toArray()).build();
    PageResponse<Service> res = serviceResourceService.list(req, false, false, false, null);
    // This Ids are wrong
    List<String> idsPresent = res.stream().map(Service::getUuid).collect(Collectors.toList());
    checkForInvalidIds(ids, idsPresent);
  }

  private void checkEnvExists(Set<String> envIds) {
    if (isEmpty(envIds)) {
      return;
    }
    List<String> ids = new ArrayList<>(envIds);
    PageRequest<Environment> req =
        aPageRequest().addFieldsIncluded("_id").addFilter("_id", IN, envIds.toArray()).build();
    PageResponse<Environment> res = environmentService.list(req, false, false, null);
    // This Ids are wrong
    List<String> idsPresent = res.stream().map(Environment::getUuid).collect(Collectors.toList());
    checkForInvalidIds(ids, idsPresent);
  }

  private void checkProvisionerExists(Set<String> provisionersIds) {
    if (isEmpty(provisionersIds)) {
      return;
    }
    List<String> ids = new ArrayList<>(provisionersIds);
    PageRequest<InfrastructureProvisioner> req =
        aPageRequest().addFieldsIncluded("_id").addFilter("_id", IN, provisionersIds.toArray()).build();
    PageResponse<InfrastructureProvisioner> res = infrastructureProvisionerService.list(req);
    // This Ids are wrong
    List<String> idsPresent = res.stream().map(InfrastructureProvisioner::getUuid).collect(Collectors.toList());
    checkForInvalidIds(ids, idsPresent);
  }

  private void checkWhetherIdsAreCorrect(QLAppPermissions appPermissions) {
    checkApplicationsExists(appPermissions.getApplications().getAppIds());
    switch (appPermissions.getPermissionType()) {
      case SERVICE:
        checkServiceExists(appPermissions.getServices().getServiceIds());
        break;
      case ENV:
        checkEnvExists(appPermissions.getEnvironments().getEnvIds());
        break;
      case WORKFLOW:
        checkEnvExists(appPermissions.getWorkflows().getEnvIds());
        break;
      case PIPELINE:
        checkEnvExists(appPermissions.getPipelines().getEnvIds());
        break;
      case DEPLOYMENT:
        checkEnvExists(appPermissions.getDeployments().getEnvIds());
        break;
      case PROVISIONER:
        checkProvisionerExists(appPermissions.getProvisioners().getProvisionerIds());
        break;
      default:
        break;
    }
  }

  private void validateTheActions(QLPermissionType permissionType, Set<QLActions> actions) {
    // If no actions is provided, we will ask the user to give the actions field
    if (isEmpty(actions)) {
      throw new InvalidRequestException(
          String.format("No Actions Supplied for the %s permission type", permissionType.getStringValue()));
    }
    if (permissionType != QLPermissionType.ALL) {
      if (permissionType == QLPermissionType.DEPLOYMENT) {
        if (actions.contains(QLActions.CREATE)) {
          throw new InvalidRequestException(
              String.format("Invalid action CREATE for the %s permission type", permissionType.getStringValue()));
        }
        if (actions.contains(QLActions.UPDATE)) {
          throw new InvalidRequestException(
              String.format("Invalid action UPDATE for the %s permission type", permissionType.getStringValue()));
        }
        if (actions.contains(QLActions.DELETE)) {
          throw new InvalidRequestException(
              String.format("Invalid action DELETE for the %s permission type", permissionType.getStringValue()));
        }
      } else {
        // All other PermissionType doesn't support the execute operation
        if (actions.contains(QLActions.EXECUTE)) {
          throw new InvalidRequestException(
              String.format("Invalid action EXECUTE  for the %s permission type", permissionType.getStringValue()));
        }
      }
    }
  }

  private void checkThePermissionFilterisNotNull(QLAppPermissions appPermission) {
    switch (appPermission.getPermissionType()) {
      case ALL:
        return;
      case SERVICE:
        if (appPermission.getServices() != null) {
          if (appPermission.getServices().getServiceIds() != null
              || appPermission.getServices().getFilterType() != null) {
            return;
          }
        }
        break;
      case ENV:
        if (appPermission.getEnvironments() != null) {
          if (appPermission.getEnvironments().getEnvIds() != null
              || appPermission.getEnvironments().getFilterTypes() != null) {
            return;
          }
        }
        break;
      case WORKFLOW:
        if (appPermission.getWorkflows() != null) {
          if (appPermission.getWorkflows().getEnvIds() != null
              || appPermission.getWorkflows().getFilterTypes() != null) {
            return;
          }
        }
        break;
      case PIPELINE:
        if (appPermission.getPipelines() != null) {
          if (appPermission.getPipelines().getEnvIds() != null
              || appPermission.getPipelines().getFilterTypes() != null) {
            return;
          }
        }
        break;
      case DEPLOYMENT:
        if (appPermission.getDeployments() != null) {
          if (appPermission.getDeployments().getEnvIds() != null
              || appPermission.getDeployments().getFilterTypes() != null) {
            return;
          }
        }
        break;
      case PROVISIONER:
        if (appPermission.getProvisioners() != null) {
          if (appPermission.getProvisioners().getProvisionerIds() != null
              || appPermission.getProvisioners().getFilterType() != null) {
            return;
          }
        }
        break;
      default:
        throw new InvalidRequestException("Invalid PermissionType Given by the user");
    }
    throw new InvalidRequestException(
        String.format("Invalid filter given in %s permissionType", appPermission.getPermissionType().getStringValue()));
  }

  private void checkWhetherPermissionIsValid(QLAppPermissions appPermission) {
    // Check that the appFilter should not be NULL
    QLAppFilter application = appPermission.getApplications();
    if (appPermission.getPermissionType() == null) {
      throw new InvalidRequestException("No permission type given in the Application Permission");
    }
    if (application == null) {
      throw new InvalidRequestException(String.format(
          "No applications provided in the %s permission type", appPermission.getPermissionType().getStringValue()));
    }
    if (application.getAppIds() == null && application.getFilterType() == null) {
      throw new InvalidRequestException(
          String.format("No appIds or filterType provided for the applications in %s permission Type",
              appPermission.getPermissionType()));
    }
    checkThePermissionFilterisNotNull(appPermission);
  }

  private void checkAllAppPermissionFilter(QLAppPermissions appPermission) {
    // If the user has given the ids, then we won't consider the filterType thus no need to check for filterType All
    QLAppFilter application = appPermission.getApplications();
    if (isNotEmpty(application.getAppIds())) {
      return;
    }
    // If ids is empty then filterType will be there as we already did a check for it, and it can only take value ALL
    switch (appPermission.getPermissionType()) {
      case ALL:
        return;
      case SERVICE:
        if (appPermission.getServices().getServiceIds() == null) {
          return;
        }
        break;
      case ENV:
        if (appPermission.getEnvironments().getEnvIds() == null) {
          return;
        }
        break;
      case WORKFLOW:
        if (appPermission.getWorkflows().getEnvIds() == null) {
          return;
        }
        break;
      case PIPELINE:
        if (appPermission.getPipelines().getEnvIds() == null) {
          return;
        }
        break;
      case DEPLOYMENT:
        if (appPermission.getDeployments().getEnvIds() == null) {
          return;
        }
        break;
      case PROVISIONER:
        if (appPermission.getProvisioners().getProvisionerIds() == null) {
          return;
        }
        break;
      default:
        logger.info("Invalid PermissionType Given by the user");
    }
    throw new InvalidRequestException(
        String.format("%s Ids should not be supplied with AppFilter=\"ALL Applications\" for filterType %s",
            appPermission.getPermissionType(), appPermission.getPermissionType()));
  }

  // it is clear form where the error is happening
  public void validatePermission(QLSetUserGroupPermissionsParameters parameters) {
    // check if the userGroup Exists
    if (parameters.getPermissions().getAppPermissions() == null) {
      return;
    }
    for (QLAppPermissions appPermission : parameters.getPermissions().getAppPermissions()) {
      // Check that for a particular permissionType their filterType should also be given
      checkWhetherPermissionIsValid(appPermission);
      // Check that the action is valid for that permissionType
      validateTheActions(appPermission.getPermissionType(), appPermission.getActions());
      // Check that ids should not be supplied when filterType ALL is selected for the application
      checkAllAppPermissionFilter(appPermission);
      // Check that the user supplied the correct id
      checkWhetherIdsAreCorrect(appPermission);
    }
  }
}
