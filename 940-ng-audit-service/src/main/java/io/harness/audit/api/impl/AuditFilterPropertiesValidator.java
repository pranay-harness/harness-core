package io.harness.audit.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.beans.Principal;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.Resource;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.scope.ResourceScope;

import com.google.inject.Inject;
import java.util.List;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AuditFilterPropertiesValidator {
  public void validate(String accountIdentifier, AuditFilterPropertiesDTO auditFilterPropertiesDTO) {
    validateFilterRequest(accountIdentifier, auditFilterPropertiesDTO);
  }

  private void validateFilterRequest(String accountIdentifier, AuditFilterPropertiesDTO auditFilterPropertiesDTO) {
    if (hasNone(accountIdentifier)) {
      throw new InvalidRequestException("Missing accountIdentifier in the audit filter request");
    }
    if (auditFilterPropertiesDTO == null) {
      return;
    }
    if (hasSome(auditFilterPropertiesDTO.getScopes())) {
      verifyScopes(accountIdentifier, auditFilterPropertiesDTO.getScopes());
    }
    if (hasSome(auditFilterPropertiesDTO.getResources())) {
      verifyResources(auditFilterPropertiesDTO.getResources());
    }
    if (hasSome(auditFilterPropertiesDTO.getPrincipals())) {
      verifyPrincipals(auditFilterPropertiesDTO.getPrincipals());
    }
    verifyTimeFilter(auditFilterPropertiesDTO.getStartTime(), auditFilterPropertiesDTO.getEndTime());
  }

  private void verifyPrincipals(List<Principal> principals) {
    principals.forEach(principal -> {
      if (principal.getType() == null) {
        throw new InvalidRequestException("Invalid principal filter with missing principal type.");
      }
    });
  }

  private void verifyScopes(String accountIdentifier, List<ResourceScope> resourceScopes) {
    if (hasSome(resourceScopes)) {
      resourceScopes.forEach(resourceScope -> verifyScope(accountIdentifier, resourceScope));
    }
  }

  private void verifyScope(String accountIdentifier, ResourceScope resourceScope) {
    if (!accountIdentifier.equals(resourceScope.getAccountIdentifier())) {
      throw new InvalidRequestException(String.format(
          "Invalid resource scope filter with accountIdentifier %s.", resourceScope.getAccountIdentifier()));
    }
    if (hasNone(resourceScope.getOrgIdentifier()) && hasSome(resourceScope.getProjectIdentifier())) {
      throw new InvalidRequestException(
          String.format("Invalid resource scope filter with projectIdentifier %s but missing orgIdentifier.",
              resourceScope.getProjectIdentifier()));
    }
  }

  private void verifyResources(List<Resource> resources) {
    if (hasSome(resources)) {
      resources.forEach(this::verifyResource);
    }
  }

  private void verifyResource(Resource resource) {
    if (hasNone(resource.getType())) {
      throw new InvalidRequestException("Invalid resource filter with missing resource type.");
    }
    List<KeyValuePair> labels = resource.getLabels();
    if (hasSome(labels)) {
      labels.forEach(label -> {
        if (hasNone(label.getKey())) {
          throw new InvalidRequestException("Invalid resource filter with missing key in resource labels.");
        }
        if (hasNone(label.getValue())) {
          throw new InvalidRequestException("Invalid resource filter with missing value in resource labels.");
        }
      });
    }
  }

  private void verifyTimeFilter(Long startTime, Long endTime) {
    if (startTime != null && startTime < 0) {
      throw new InvalidRequestException(
          String.format("Invalid time filter with start time %d less than zero.", startTime));
    }
    if (endTime != null && endTime < 0) {
      throw new InvalidRequestException(String.format("Invalid time filter with end time %d less than zero.", endTime));
    }
    if (startTime != null && endTime != null && startTime > endTime) {
      throw new InvalidRequestException(
          String.format("Invalid time filter with start time %d after end time %d.", startTime, endTime));
    }
  }
}
