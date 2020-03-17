package software.wings.graphql.datafetcher.userGroup;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.graphql.datafetcher.application.AppFilterController;
import software.wings.graphql.datafetcher.environment.EnvFilterController;
import software.wings.graphql.schema.type.QLAppFilter;
import software.wings.graphql.schema.type.permissions.QLActions;
import software.wings.graphql.schema.type.permissions.QLAppPermission;
import software.wings.graphql.schema.type.permissions.QLPermissionType;
import software.wings.graphql.schema.type.permissions.QLUserGroupPermissions;
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
@Singleton
public class UserGroupPermissionValidator {
  @Inject EnvFilterController envFilterController;
  @Inject ServiceResourceService serviceResourceService;
  @Inject InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject AppFilterController appFilterController;

  private void checkForInvalidIds(List<String> idsInput, List<String> idsPresent) {
    idsInput.removeAll(idsPresent);
    if (isNotEmpty(idsInput)) {
      throw new InvalidRequestException(
          String.format("Invalid id/s %s provided in the request", String.join(", ", idsInput)));
    }
  }

  private void checkServiceExists(Set<String> serviceIds, String accountId) {
    if (isEmpty(serviceIds)) {
      return;
    }
    List<String> ids = new ArrayList<>(serviceIds);
    PageRequest<Service> req = aPageRequest()
                                   .addFieldsIncluded("_id")
                                   .addFilter("accountId", SearchFilter.Operator.EQ, accountId)
                                   .addFilter("_id", IN, serviceIds.toArray())
                                   .build();
    PageResponse<Service> res = serviceResourceService.list(req, false, false, false, null);
    // This Ids are wrong
    List<String> idsPresent = res.stream().map(Service::getUuid).collect(Collectors.toList());
    checkForInvalidIds(ids, idsPresent);
  }

  private void checkProvisionerExists(Set<String> provisionersIds, String accountId) {
    if (isEmpty(provisionersIds)) {
      return;
    }
    List<String> ids = new ArrayList<>(provisionersIds);
    PageRequest<InfrastructureProvisioner> req = aPageRequest()
                                                     .addFieldsIncluded("_id")
                                                     .addFilter("accountId", SearchFilter.Operator.EQ, accountId)
                                                     .addFilter("_id", IN, provisionersIds.toArray())
                                                     .build();
    PageResponse<InfrastructureProvisioner> res = infrastructureProvisionerService.list(req);
    // This Ids are wrong
    List<String> idsPresent = res.stream().map(InfrastructureProvisioner::getUuid).collect(Collectors.toList());
    checkForInvalidIds(ids, idsPresent);
  }

  private void checkWhetherIdsAreCorrect(QLAppPermission appPermissions, String accountId) {
    appFilterController.checkApplicationsExists(appPermissions.getApplications().getAppIds(), accountId);
    switch (appPermissions.getPermissionType()) {
      case SERVICE:
        checkServiceExists(appPermissions.getServices().getServiceIds(), accountId);
        break;
      case ENV:
        envFilterController.checkEnvExists(appPermissions.getEnvironments().getEnvIds(), accountId);
        break;
      case WORKFLOW:
        envFilterController.checkEnvExists(appPermissions.getWorkflows().getEnvIds(), accountId);
        break;
      case PIPELINE:
        envFilterController.checkEnvExists(appPermissions.getPipelines().getEnvIds(), accountId);
        break;
      case DEPLOYMENT:
        envFilterController.checkEnvExists(appPermissions.getDeployments().getEnvIds(), accountId);
        break;
      case PROVISIONER:
        checkProvisionerExists(appPermissions.getProvisioners().getProvisionerIds(), accountId);
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

  private void checkThePermissionFilterisNotNull(QLAppPermission appPermission) {
    switch (appPermission.getPermissionType()) {
      case ALL:
        return;
      case SERVICE:
        if (appPermission.getServices() != null) {
          if (isNotEmpty(appPermission.getServices().getServiceIds())
              || appPermission.getServices().getFilterType() != null) {
            return;
          }
        }
        break;
      case ENV:
        if (appPermission.getEnvironments() != null) {
          if (isNotEmpty(appPermission.getEnvironments().getEnvIds())
              || isNotEmpty(appPermission.getEnvironments().getFilterTypes())) {
            return;
          }
        }
        break;
      case WORKFLOW:
        if (appPermission.getWorkflows() != null) {
          if (isNotEmpty(appPermission.getWorkflows().getEnvIds())
              || isNotEmpty(appPermission.getWorkflows().getFilterTypes())) {
            return;
          }
        }
        break;
      case PIPELINE:
        if (appPermission.getPipelines() != null) {
          if (isNotEmpty(appPermission.getPipelines().getEnvIds())
              || isNotEmpty(appPermission.getPipelines().getFilterTypes())) {
            return;
          }
        }
        break;
      case DEPLOYMENT:
        if (appPermission.getDeployments() != null) {
          if (isNotEmpty(appPermission.getDeployments().getEnvIds())
              || isNotEmpty(appPermission.getDeployments().getFilterTypes())) {
            return;
          }
        }
        break;
      case PROVISIONER:
        if (appPermission.getProvisioners() != null) {
          if (isNotEmpty(appPermission.getProvisioners().getProvisionerIds())
              || appPermission.getProvisioners().getFilterType() != null) {
            return;
          }
        }
        break;
      default:
        throw new InvalidRequestException("Invalid PermissionType Given by the user");
    }
    throw new InvalidRequestException(String.format("No %s filter provided with the permission with type %s",
        appPermission.getPermissionType(), appPermission.getPermissionType().getStringValue()));
  }

  private void checkWhetherPermissionIsValid(QLAppPermission appPermission, String accountId) {
    // Check that the appFilter should not be NULL
    QLAppFilter application = appPermission.getApplications();
    if (appPermission.getPermissionType() == null) {
      throw new InvalidRequestException("No permission type given in the Application Permission");
    }
    appFilterController.validateAppFilter(application, accountId);
    checkThePermissionFilterisNotNull(appPermission);
  }

  private void checkAllAppPermissionFilter(QLAppPermission appPermission) {
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

  public void validateAppPermission(String accountId, List<QLAppPermission> appPermissions) {
    if (isEmpty(appPermissions)) {
      return;
    }
    for (QLAppPermission appPermission : appPermissions) {
      // Check that for a particular permissionType their filterType should also be given
      checkWhetherPermissionIsValid(appPermission, accountId);
      // Check that the action is valid for that permissionType
      validateTheActions(appPermission.getPermissionType(), appPermission.getActions());
      // Check that ids should not be supplied when filterType ALL is selected for the application
      checkAllAppPermissionFilter(appPermission);
      // Check that the user supplied the correct id
      checkWhetherIdsAreCorrect(appPermission, accountId);
    }
  }

  // it is clear form where the error is happening
  public void validatePermission(QLUserGroupPermissions permissions, String accountId) {
    if (permissions == null) {
      return;
    }
    validateAppPermission(accountId, permissions.getAppPermissions());
  }
}
