package io.harness.audit.api.impl;

import static io.harness.NGCommonEntityConstants.ENVIRONMENT_IDENTIFIER_KEY;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.mapper.AuditEventMapper.fromDTO;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditService;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.beans.Principal;
import io.harness.audit.entities.AuditEvent;
import io.harness.audit.entities.AuditEvent.AuditEventKeys;
import io.harness.audit.repositories.AuditRepository;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.Resource;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.scope.ResourceScope;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AuditServiceImpl implements AuditService {
  private final AuditRepository auditRepository;

  @Override
  public AuditEvent create(AuditEventDTO auditEventDTO) {
    AuditEvent auditEvent = fromDTO(auditEventDTO);
    return auditRepository.save(auditEvent);
  }

  @Override
  public Page<AuditEvent> list(
      String accountIdentifier, PageRequest pageRequest, AuditFilterPropertiesDTO auditFilterPropertiesDTO) {
    validateFilterRequest(accountIdentifier, auditFilterPropertiesDTO);
    Criteria criteria = getFilterCriteria(accountIdentifier, auditFilterPropertiesDTO);
    return auditRepository.findAll(criteria, getPageRequest(pageRequest));
  }

  private Criteria getFilterCriteria(String accountIdentifier, AuditFilterPropertiesDTO auditFilterPropertiesDTO) {
    List<Criteria> criteriaList = new ArrayList<>();
    criteriaList.add(getBaseScopeCriteria(accountIdentifier));
    if (auditFilterPropertiesDTO == null) {
      return criteriaList.get(0);
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getScopes())) {
      criteriaList.add(getScopeCriteria(auditFilterPropertiesDTO.getScopes()));
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getResources())) {
      criteriaList.add(getResourceCriteria(auditFilterPropertiesDTO.getResources()));
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getModuleTypes())) {
      criteriaList.add(Criteria.where(AuditEventKeys.moduleType).in(auditFilterPropertiesDTO.getModuleTypes()));
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getActions())) {
      criteriaList.add(Criteria.where(AuditEventKeys.action).in(auditFilterPropertiesDTO.getActions()));
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getEnvironmentIdentifiers())) {
      criteriaList.add(Criteria.where(AuditEventKeys.RESOURCE_LABEL_KEYS_KEY)
                           .is(ENVIRONMENT_IDENTIFIER_KEY)
                           .and(AuditEventKeys.RESOURCE_LABEL_VALUES_KEY)
                           .in(auditFilterPropertiesDTO.getEnvironmentIdentifiers()));
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getPrincipals())) {
      criteriaList.add(getPrincipalCriteria(auditFilterPropertiesDTO.getPrincipals()));
    }
    if (auditFilterPropertiesDTO.getStartTime() != null) {
      criteriaList.add(Criteria.where(AuditEventKeys.timestamp).gte(auditFilterPropertiesDTO.getStartTime()));
    }
    if (auditFilterPropertiesDTO.getEndTime() != null) {
      criteriaList.add(Criteria.where(AuditEventKeys.timestamp).lte(auditFilterPropertiesDTO.getEndTime()));
    }
    return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
  }

  private Criteria getBaseScopeCriteria(String accountIdentifier) {
    return Criteria.where(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY).is(accountIdentifier);
  }

  private Criteria getScopeCriteria(List<ResourceScope> resourceScopes) {
    List<Criteria> criteriaList = new ArrayList<>();
    resourceScopes.forEach(resourceScope -> {
      Criteria criteria =
          Criteria.where(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY).is(resourceScope.getAccountIdentifier());
      if (isNotEmpty(resourceScope.getOrgIdentifier())) {
        criteria.and(AuditEventKeys.ORG_IDENTIFIER_KEY).is(resourceScope.getOrgIdentifier());
        if (isNotEmpty(resourceScope.getProjectIdentifier())) {
          criteria.and(AuditEventKeys.PROJECT_IDENTIFIER_KEY).is(resourceScope.getProjectIdentifier());
        }
      }
      List<KeyValuePair> labels = resourceScope.getLabels();
      if (isNotEmpty(labels)) {
        labels.forEach(label
            -> criteria.and(AuditEventKeys.RESOURCE_SCOPE_LABEL_KEYS_KEY)
                   .is(label.getKey())
                   .and(AuditEventKeys.RESOURCE_SCOPE_LABEL_VALUES_KEY)
                   .is(label.getValue()));
      }
      criteriaList.add(criteria);
    });
    return new Criteria().orOperator(criteriaList.toArray(new Criteria[0]));
  }

  private Criteria getResourceCriteria(List<Resource> resources) {
    List<Criteria> criteriaList = new ArrayList<>();
    resources.forEach(resource -> {
      Criteria criteria = Criteria.where(AuditEventKeys.RESOURCE_TYPE_KEY).is(resource.getType());
      if (isNotEmpty(resource.getIdentifier())) {
        criteria.and(AuditEventKeys.RESOURCE_IDENTIFIER_KEY).is(resource.getIdentifier());
      }
      List<KeyValuePair> labels = resource.getLabels();
      if (isNotEmpty(labels)) {
        labels.forEach(label
            -> criteria.and(AuditEventKeys.RESOURCE_LABEL_KEYS_KEY)
                   .is(label.getKey())
                   .and(AuditEventKeys.RESOURCE_LABEL_VALUES_KEY)
                   .is(label.getValue()));
      }
      criteriaList.add(criteria);
    });
    return new Criteria().orOperator(criteriaList.toArray(new Criteria[0]));
  }

  private Criteria getPrincipalCriteria(List<Principal> principals) {
    List<Criteria> criteriaList = new ArrayList<>();
    principals.forEach(principal -> {
      Criteria criteria = Criteria.where(AuditEventKeys.PRINCIPAL_TYPE_KEY).is(principal.getType());
      if (isNotEmpty(principal.getIdentifier())) {
        criteria.and(AuditEventKeys.PRINCIPAL_IDENTIFIER_KEY).is(principal.getIdentifier());
      }
      criteriaList.add(criteria);
    });
    return new Criteria().orOperator(criteriaList.toArray(new Criteria[0]));
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

    if (auditFilterPropertiesDTO.getStartTime() != null && auditFilterPropertiesDTO.getEndTime() != null
        && auditFilterPropertiesDTO.getStartTime() > auditFilterPropertiesDTO.getEndTime()) {
      throw new InvalidRequestException(String.format("Invalid time filter with start time %d after end time %d.",
          auditFilterPropertiesDTO.getStartTime(), auditFilterPropertiesDTO.getEndTime()));
    }
  }

  private void verifyPrincipals(List<Principal> principals) {
    principals.forEach(principal -> {
      if (principal.getType() == null) {
        throw new InvalidRequestException("Invalid principal filter with missing principal type.");
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
      throw new InvalidRequestException(
          String.format("Invalid resource scope filter with accountIdentifier %s.", accountIdentifier));
    }
    if (isEmpty(resourceScope.getOrgIdentifier()) && isNotEmpty(resourceScope.getProjectIdentifier())) {
      throw new InvalidRequestException(
          String.format("Invalid resource scope filter with projectIdentifier %s but missing orgIdentifier.",
              resourceScope.getProjectIdentifier()));
    }
    List<KeyValuePair> labels = resourceScope.getLabels();
    if (isNotEmpty(labels)) {
      labels.forEach(label -> {
        if (isEmpty(label.getKey())) {
          throw new InvalidRequestException("Invalid resource scope filter with missing key in resource scope labels.");
        }
        if (isEmpty(label.getValue())) {
          throw new InvalidRequestException(
              "Invalid resource scope filter with missing value in resource scope labels.");
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
    List<KeyValuePair> labels = resource.getLabels();
    if (isNotEmpty(labels)) {
      labels.forEach(label -> {
        if (isEmpty(label.getKey())) {
          throw new InvalidRequestException("Invalid resource filter with missing key in resource labels.");
        }
        if (isEmpty(label.getValue())) {
          throw new InvalidRequestException("Invalid resource filter with missing value in resource labels.");
        }
      });
    }
  }
}
