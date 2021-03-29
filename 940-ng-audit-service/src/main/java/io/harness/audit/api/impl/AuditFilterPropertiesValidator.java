package io.harness.audit.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.beans.Principal;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.Resource;
import io.harness.scope.ResourceScope;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AuditFilterPropertiesValidator {
  public void validate(String accountIdentifier, AuditFilterPropertiesDTO auditFilterPropertiesDTO) {
    validateFilterRequest(accountIdentifier, auditFilterPropertiesDTO);
  }

  private void validateFilterRequest(String accountIdentifier, AuditFilterPropertiesDTO auditFilterPropertiesDTO) {
    if (isEmpty(accountIdentifier)) {
      throw new InvalidRequestException("Missing accountIdentifier in the audit filter request");
    }
    if (auditFilterPropertiesDTO == null) {
      return;
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getScopes())) {
      verifyScopes(accountIdentifier, auditFilterPropertiesDTO.getScopes());
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getResources())) {
      verifyResources(auditFilterPropertiesDTO.getResources());
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getPrincipals())) {
      verifyPrincipals(auditFilterPropertiesDTO.getPrincipals());
    }
    verifyTimeFilter(auditFilterPropertiesDTO.getStartTime(), auditFilterPropertiesDTO.getEndTime());
  }

  private void verifyPrincipals(List<Principal> principals) {
    principals.forEach(principal -> {
      if (principal.getType() == null) {
        throw new InvalidRequestException("Invalid principal filter with missing principal type.");
      }
      if (isEmpty(principal.getIdentifier())) {
        throw new InvalidRequestException("Invalid principal filter with missing principal identifier.");
      }
    });
  }

  private void verifyScopes(String accountIdentifier, List<ResourceScope> resourceScopes) {
    if (isNotEmpty(resourceScopes)) {
      resourceScopes.forEach(resourceScope -> verifyScope(accountIdentifier, resourceScope));
    }
  }

  private void verifyScope(String accountIdentifier, ResourceScope resourceScope) {
    if (!accountIdentifier.equals(resourceScope.getAccountIdentifier())) {
      throw new InvalidRequestException(String.format(
          "Invalid resource scope filter with accountIdentifier %s.", resourceScope.getAccountIdentifier()));
    }
    if (isEmpty(resourceScope.getOrgIdentifier()) && isNotEmpty(resourceScope.getProjectIdentifier())) {
      throw new InvalidRequestException(
          String.format("Invalid resource scope filter with projectIdentifier %s but missing orgIdentifier.",
              resourceScope.getProjectIdentifier()));
    }
    if (isEmpty(resourceScope.getOrgIdentifier()) && isNotEmpty(resourceScope.getLabels())) {
      throw new InvalidRequestException("Invalid resource scope filter with labels present but missing orgIdentifier.");
    }
    if (isEmpty(resourceScope.getProjectIdentifier()) && isNotEmpty(resourceScope.getLabels())) {
      throw new InvalidRequestException(
          "Invalid resource scope filter with labels present but missing projectIdentifier.");
    }
    Map<String, String> labels = resourceScope.getLabels();
    if (isNotEmpty(labels)) {
      labels.forEach((key, value) -> {
        if (isEmpty(key)) {
          throw new InvalidRequestException("Invalid resource scope filter with missing key in resource scope labels.");
        }
        if (isEmpty(value)) {
          throw new InvalidRequestException(
              "Invalid resource scope filter with missing value in resource scope labels.");
        }
        if (NGCommonEntityConstants.ACCOUNT_KEY.equals(key)) {
          throw new InvalidRequestException(
              "Invalid resource scope filter with key as accountIdentifier in resource scope labels.");
        }
      });
    }
  }

  private void verifyResources(List<Resource> resources) {
    if (isNotEmpty(resources)) {
      resources.forEach(this::verifyResource);
    }
  }

  private void verifyResource(Resource resource) {
    if (isEmpty(resource.getType())) {
      throw new InvalidRequestException("Invalid resource filter with missing resource type.");
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
