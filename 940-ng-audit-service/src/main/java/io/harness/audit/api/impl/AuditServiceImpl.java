package io.harness.audit.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.mapper.AuditEventMapper.fromDTO;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.System.currentTimeMillis;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditService;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.beans.Principal;
import io.harness.audit.beans.Resource;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScope;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.entities.AuditEvent;
import io.harness.audit.entities.AuditEvent.AuditEventKeys;
import io.harness.audit.mapper.ResourceMapper;
import io.harness.audit.mapper.ResourceScopeMapper;
import io.harness.audit.repositories.AuditRepository;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.ng.core.common.beans.KeyValuePair.KeyValuePairKeys;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class AuditServiceImpl implements AuditService {
  private final AuditRepository auditRepository;
  private final AuditFilterPropertiesValidator auditFilterPropertiesValidator;

  @Override
  public Boolean create(AuditEventDTO auditEventDTO) {
    AuditEvent auditEvent = fromDTO(auditEventDTO);
    try {
      auditRepository.save(auditEvent);
      return true;
    } catch (DuplicateKeyException ex) {
      log.info("Audit for this entry already exists with id {} and account identifier {}", auditEvent.getInsertId(),
          auditEvent.getResourceScope().getAccountIdentifier());
      return true;
    }
  }

  @Override
  public Page<AuditEvent> list(
      String accountIdentifier, PageRequest pageRequest, AuditFilterPropertiesDTO auditFilterPropertiesDTO) {
    auditFilterPropertiesValidator.validate(accountIdentifier, auditFilterPropertiesDTO);
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
    if (isNotEmpty(auditFilterPropertiesDTO.getModules())) {
      criteriaList.add(Criteria.where(AuditEventKeys.module).in(auditFilterPropertiesDTO.getModules()));
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getActions())) {
      criteriaList.add(Criteria.where(AuditEventKeys.action).in(auditFilterPropertiesDTO.getActions()));
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getEnvironmentIdentifiers())) {
      criteriaList.add(Criteria.where(AuditEventKeys.environmentIdentifier)
                           .in(auditFilterPropertiesDTO.getEnvironmentIdentifiers()));
    }
    if (isNotEmpty(auditFilterPropertiesDTO.getPrincipals())) {
      criteriaList.add(getPrincipalCriteria(auditFilterPropertiesDTO.getPrincipals()));
    }
    criteriaList.add(
        Criteria.where(AuditEventKeys.timestamp)
            .gte(auditFilterPropertiesDTO.getStartTime() == null ? 0 : auditFilterPropertiesDTO.getStartTime()));
    criteriaList.add(Criteria.where(AuditEventKeys.timestamp)
                         .lte(auditFilterPropertiesDTO.getEndTime() == null ? currentTimeMillis()
                                                                            : auditFilterPropertiesDTO.getEndTime()));
    return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
  }

  private Criteria getBaseScopeCriteria(String accountIdentifier) {
    return Criteria.where(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY).is(accountIdentifier);
  }

  private Criteria getScopeCriteria(List<ResourceScopeDTO> resourceScopes) {
    List<Criteria> criteriaList = new ArrayList<>();
    resourceScopes.forEach(resourceScope -> {
      Criteria criteria =
          Criteria.where(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY).is(resourceScope.getAccountIdentifier());
      if (isNotEmpty(resourceScope.getOrgIdentifier())) {
        criteria.and(AuditEventKeys.ORG_IDENTIFIER_KEY).is(resourceScope.getOrgIdentifier());
        if (isNotEmpty(resourceScope.getProjectIdentifier())) {
          criteria.and(AuditEventKeys.PROJECT_IDENTIFIER_KEY).is(resourceScope.getProjectIdentifier());
          ResourceScope dbo = ResourceScopeMapper.fromDTO(resourceScope);
          List<KeyValuePair> labels = dbo.getLabels();
          if (isNotEmpty(labels)) {
            List<Criteria> labelsCriteria = new ArrayList<>();
            labels.forEach(label
                -> labelsCriteria.add(Criteria.where(AuditEventKeys.RESOURCE_SCOPE_LABEL_KEY)
                                          .elemMatch(Criteria.where(KeyValuePairKeys.key)
                                                         .is(label.getKey())
                                                         .and(KeyValuePairKeys.value)
                                                         .is(label.getValue()))));
            criteria.andOperator(labelsCriteria.toArray(new Criteria[0]));
          }
        }
      }
      criteriaList.add(criteria);
    });
    return new Criteria().orOperator(criteriaList.toArray(new Criteria[0]));
  }

  private Criteria getResourceCriteria(List<ResourceDTO> resources) {
    List<Criteria> criteriaList = new ArrayList<>();
    resources.forEach(resource -> {
      Criteria criteria = Criteria.where(AuditEventKeys.RESOURCE_TYPE_KEY).is(resource.getType());
      if (isNotEmpty(resource.getIdentifier())) {
        criteria.and(AuditEventKeys.RESOURCE_IDENTIFIER_KEY).is(resource.getIdentifier());
      }
      Resource dbo = ResourceMapper.fromDTO(resource);
      List<KeyValuePair> labels = dbo.getLabels();
      if (isNotEmpty(labels)) {
        List<Criteria> labelsCriteria = new ArrayList<>();
        labels.forEach(label
            -> labelsCriteria.add(Criteria.where(AuditEventKeys.RESOURCE_LABEL_KEY)
                                      .elemMatch(Criteria.where(KeyValuePairKeys.key)
                                                     .is(label.getKey())
                                                     .and(KeyValuePairKeys.value)
                                                     .is(label.getValue()))));
        criteria.andOperator(labelsCriteria.toArray(new Criteria[0]));
      }

      criteriaList.add(criteria);
    });
    return new Criteria().orOperator(criteriaList.toArray(new Criteria[0]));
  }

  private Criteria getPrincipalCriteria(List<Principal> principals) {
    List<Criteria> criteriaList = new ArrayList<>();
    principals.forEach(principal -> {
      Criteria criteria = Criteria.where(AuditEventKeys.PRINCIPAL_TYPE_KEY)
                              .is(principal.getType())
                              .and(AuditEventKeys.PRINCIPAL_IDENTIFIER_KEY)
                              .is(principal.getIdentifier());
      criteriaList.add(criteria);
    });
    return new Criteria().orOperator(criteriaList.toArray(new Criteria[0]));
  }
}
