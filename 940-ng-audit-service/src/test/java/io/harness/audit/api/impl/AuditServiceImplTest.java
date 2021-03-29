package io.harness.audit.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.utils.PageTestUtils.getPage;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.AuditCommonConstants;
import io.harness.audit.api.AuditService;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.beans.Principal;
import io.harness.audit.beans.PrincipalType;
import io.harness.audit.entities.AuditEvent;
import io.harness.audit.entities.AuditEvent.AuditEventKeys;
import io.harness.audit.repositories.AuditRepository;
import io.harness.category.element.UnitTests;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.Resource;
import io.harness.ng.core.common.beans.KeyValuePair.KeyValuePairKeys;
import io.harness.rule.Owner;
import io.harness.scope.ResourceScope;

import com.mongodb.BasicDBList;
import java.util.List;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public class AuditServiceImplTest extends CategoryTest {
  private AuditRepository auditRepository;
  private AuditFilterPropertiesValidator auditFilterPropertiesValidator;
  private AuditService auditService;
  private final PageRequest samplePageRequest = PageRequest.builder().pageIndex(0).pageSize(50).build();
  private static final String MONGO_OR_OPERATOR = "$or";
  private static final String MONGO_AND_OPERATOR = "$and";
  private static final String MONGO_ELEM_MATCH_OPERATOR = "$elemMatch";
  private static final String MONGO_GTE_OPERATOR = "$gte";
  private static final String MONGO_LTE_OPERATOR = "$lte";
  private static final String MONGO_IN_OPERATOR = "$in";

  @Before
  public void setup() {
    auditRepository = mock(AuditRepository.class);
    auditFilterPropertiesValidator = mock(AuditFilterPropertiesValidator.class);
    auditService = spy(new AuditServiceImpl(auditRepository, auditFilterPropertiesValidator));
    doNothing().when(auditFilterPropertiesValidator).validate(any(), any());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testNullAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(auditRepository.findAll(any(Criteria.class), any(Pageable.class))).thenReturn(getPage(emptyList(), 0));
    Page<AuditEvent> auditEvents = auditService.list(accountIdentifier, samplePageRequest, null);
    verify(auditRepository, times(1)).findAll(criteriaArgumentCaptor.capture(), any(Pageable.class));
    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertNotNull(auditEvents);
    assertNotNull(criteria);
    assertEquals(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY, criteria.getKey());
    assertEquals(accountIdentifier, criteria.getCriteriaObject().getString(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testScopeAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(auditRepository.findAll(any(Criteria.class), any(Pageable.class))).thenReturn(getPage(emptyList(), 0));
    AuditFilterPropertiesDTO correctFilter =
        AuditFilterPropertiesDTO.builder()
            .scopes(singletonList(
                ResourceScope.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build()))
            .build();
    Page<AuditEvent> auditEvents = auditService.list(accountIdentifier, samplePageRequest, correctFilter);
    verify(auditRepository, times(1)).findAll(criteriaArgumentCaptor.capture(), any(Pageable.class));
    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertNotNull(auditEvents);
    assertNotNull(criteria);
    BasicDBList andList = (BasicDBList) criteria.getCriteriaObject().get(MONGO_AND_OPERATOR);
    assertNotNull(andList);
    assertEquals(4, andList.size());
    Document accountDocument = (Document) andList.get(0);
    assertEquals(accountIdentifier, accountDocument.getString(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));

    Document scopeDocument = (Document) andList.get(1);
    BasicDBList orList = (BasicDBList) scopeDocument.get(MONGO_OR_OPERATOR);
    assertNotNull(orList);
    Document accountOrgScopeDocument = (Document) orList.get(0);
    assertEquals(2, accountOrgScopeDocument.size());
    assertEquals(accountIdentifier, accountOrgScopeDocument.get(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));
    BasicDBList labelsList = (BasicDBList) accountOrgScopeDocument.get(MONGO_AND_OPERATOR);
    assertEquals(1, labelsList.size());
    Document labelsDocument = (Document) labelsList.get(0);
    Document elemMatchDocument = (Document) labelsDocument.get(AuditEventKeys.RESOURCE_SCOPE_LABEL_KEY);
    Document orgScopeElemMatchDocument = (Document) elemMatchDocument.get(MONGO_ELEM_MATCH_OPERATOR);
    assertEquals(NGCommonEntityConstants.ORG_KEY, orgScopeElemMatchDocument.getString(KeyValuePairKeys.key));
    assertEquals(orgIdentifier, orgScopeElemMatchDocument.getString(KeyValuePairKeys.value));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testResourceAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String resourceType = randomAlphabetic(10);
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(auditRepository.findAll(any(Criteria.class), any(Pageable.class))).thenReturn(getPage(emptyList(), 0));
    AuditFilterPropertiesDTO correctFilter =
        AuditFilterPropertiesDTO.builder()
            .resources(singletonList(Resource.builder().identifier(identifier).type(resourceType).build()))
            .build();
    Page<AuditEvent> auditEvents = auditService.list(accountIdentifier, samplePageRequest, correctFilter);
    verify(auditRepository, times(1)).findAll(criteriaArgumentCaptor.capture(), any(Pageable.class));
    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertNotNull(auditEvents);
    assertNotNull(criteria);
    BasicDBList andList = (BasicDBList) criteria.getCriteriaObject().get(MONGO_AND_OPERATOR);
    assertNotNull(andList);
    assertEquals(4, andList.size());
    Document accountDocument = (Document) andList.get(0);
    assertEquals(accountIdentifier, accountDocument.getString(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));

    Document orDocument = (Document) andList.get(1);
    BasicDBList orList = (BasicDBList) orDocument.get(MONGO_OR_OPERATOR);
    assertEquals(1, orList.size());
    Document andLabelsDocument = (Document) orList.get(0);
    BasicDBList andLabelsList = (BasicDBList) andLabelsDocument.get(MONGO_AND_OPERATOR);
    assertEquals(2, andLabelsList.size());

    Document typeDocument = (Document) andLabelsList.get(0);
    Document labelsTypeDocument = (Document) typeDocument.get(AuditEventKeys.RESOURCE_LABEL_KEY);
    Document elemMatchLabelsTypeDocument = (Document) labelsTypeDocument.get(MONGO_ELEM_MATCH_OPERATOR);
    assertEquals(AuditCommonConstants.TYPE, elemMatchLabelsTypeDocument.getString(KeyValuePairKeys.key));
    assertEquals(resourceType, elemMatchLabelsTypeDocument.getString(KeyValuePairKeys.value));

    Document identifierDocument = (Document) andLabelsList.get(1);
    Document labelsIdentifierDocument = (Document) identifierDocument.get(AuditEventKeys.RESOURCE_LABEL_KEY);
    Document elemMatchLabelsIdentifierDocument = (Document) labelsIdentifierDocument.get(MONGO_ELEM_MATCH_OPERATOR);
    assertEquals(AuditCommonConstants.IDENTIFIER, elemMatchLabelsIdentifierDocument.getString(KeyValuePairKeys.key));
    assertEquals(identifier, elemMatchLabelsIdentifierDocument.getString(KeyValuePairKeys.value));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testPrincipalAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    PrincipalType principalType = PrincipalType.USER;
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(auditRepository.findAll(any(Criteria.class), any(Pageable.class))).thenReturn(getPage(emptyList(), 0));
    AuditFilterPropertiesDTO correctFilter =
        AuditFilterPropertiesDTO.builder()
            .principals(singletonList(Principal.builder().identifier(identifier).type(PrincipalType.USER).build()))
            .build();
    Page<AuditEvent> auditEvents = auditService.list(accountIdentifier, samplePageRequest, correctFilter);
    verify(auditRepository, times(1)).findAll(criteriaArgumentCaptor.capture(), any(Pageable.class));
    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertNotNull(auditEvents);
    assertNotNull(criteria);
    BasicDBList andList = (BasicDBList) criteria.getCriteriaObject().get(MONGO_AND_OPERATOR);
    assertNotNull(andList);
    assertEquals(4, andList.size());
    Document accountDocument = (Document) andList.get(0);
    assertEquals(accountIdentifier, accountDocument.getString(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));

    Document principalDocument = (Document) andList.get(1);
    BasicDBList orList = (BasicDBList) principalDocument.get(MONGO_OR_OPERATOR);
    assertNotNull(orList);
    Document principalTypeIdentifierScopeDocument = (Document) orList.get(0);
    assertEquals(2, principalTypeIdentifierScopeDocument.size());
    assertEquals(principalType, principalTypeIdentifierScopeDocument.get(AuditEventKeys.PRINCIPAL_TYPE_KEY));
    assertEquals(identifier, principalTypeIdentifierScopeDocument.get(AuditEventKeys.PRINCIPAL_IDENTIFIER_KEY));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testTimeAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(auditRepository.findAll(any(Criteria.class), any(Pageable.class))).thenReturn(getPage(emptyList(), 0));
    AuditFilterPropertiesDTO correctFilter = AuditFilterPropertiesDTO.builder().startTime(17L).endTime(18L).build();
    Page<AuditEvent> auditEvents = auditService.list(accountIdentifier, samplePageRequest, correctFilter);
    verify(auditRepository, times(1)).findAll(criteriaArgumentCaptor.capture(), any(Pageable.class));
    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertNotNull(auditEvents);
    assertNotNull(criteria);
    BasicDBList andList = (BasicDBList) criteria.getCriteriaObject().get(MONGO_AND_OPERATOR);
    assertNotNull(andList);
    assertEquals(3, andList.size());
    Document accountDocument = (Document) andList.get(0);
    assertEquals(accountIdentifier, accountDocument.getString(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));

    Document startTimeDocument = (Document) andList.get(1);
    assertNotNull(startTimeDocument);
    Document startTimestampDocument = (Document) startTimeDocument.get(AuditEventKeys.timestamp);
    assertNotNull(startTimestampDocument);
    assertEquals(17L, startTimestampDocument.get(MONGO_GTE_OPERATOR));

    Document endTimeDocument = (Document) andList.get(2);
    assertNotNull(endTimeDocument);
    Document endTimestampDocument = (Document) endTimeDocument.get(AuditEventKeys.timestamp);
    assertEquals(18L, endTimestampDocument.get(MONGO_LTE_OPERATOR));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testModuleTypeActionAndEnvironmentIdentifierAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    ModuleType moduleType = ModuleType.CD;
    Action action = Action.CREATE;
    String environmentIdentifier = randomAlphabetic(10);
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(auditRepository.findAll(any(Criteria.class), any(Pageable.class))).thenReturn(getPage(emptyList(), 0));
    AuditFilterPropertiesDTO correctFilter = AuditFilterPropertiesDTO.builder()
                                                 .modules(singletonList(moduleType))
                                                 .actions(singletonList(action))
                                                 .environmentIdentifiers(singletonList(environmentIdentifier))
                                                 .build();
    Page<AuditEvent> auditEvents = auditService.list(accountIdentifier, samplePageRequest, correctFilter);
    verify(auditRepository, times(1)).findAll(criteriaArgumentCaptor.capture(), any(Pageable.class));
    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertNotNull(auditEvents);
    assertNotNull(criteria);
    BasicDBList andList = (BasicDBList) criteria.getCriteriaObject().get(MONGO_AND_OPERATOR);
    assertNotNull(andList);
    assertEquals(4, andList.size());
    Document accountDocument = (Document) andList.get(0);
    assertEquals(accountIdentifier, accountDocument.getString(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));
    Document coreInfoDocument = (Document) andList.get(1);
    BasicDBList coreInfoAndList = (BasicDBList) coreInfoDocument.get(MONGO_AND_OPERATOR);
    assertEquals(3, coreInfoAndList.size());

    Document moduleCoreInfoDocument = (Document) coreInfoAndList.get(0);
    Document moduleCoreInfoElemMatchDocument = (Document) moduleCoreInfoDocument.get(AuditEventKeys.coreInfo);
    Document elemMatchModuleDocument = (Document) moduleCoreInfoElemMatchDocument.get(MONGO_ELEM_MATCH_OPERATOR);
    assertEquals(AuditCommonConstants.MODULE, elemMatchModuleDocument.getString(KeyValuePairKeys.key));
    Document moduleValueDocument = (Document) elemMatchModuleDocument.get(KeyValuePairKeys.value);
    List<String> moduleInList = (List<String>) moduleValueDocument.get(MONGO_IN_OPERATOR);
    assertEquals(1, moduleInList.size());
    assertEquals(moduleType.name(), moduleInList.get(0));

    Document actionCoreInfoDocument = (Document) coreInfoAndList.get(1);
    Document actionCoreInfoElemMatchDocument = (Document) actionCoreInfoDocument.get(AuditEventKeys.coreInfo);
    Document elemMatchActionDocument = (Document) actionCoreInfoElemMatchDocument.get(MONGO_ELEM_MATCH_OPERATOR);
    assertEquals(AuditCommonConstants.ACTION, elemMatchActionDocument.getString(KeyValuePairKeys.key));
    Document actionValueDocument = (Document) elemMatchActionDocument.get(KeyValuePairKeys.value);
    List<String> actionInList = (List<String>) actionValueDocument.get(MONGO_IN_OPERATOR);
    assertEquals(1, actionInList.size());
    assertEquals(action.name(), actionInList.get(0));

    Document environmentIdentifierCoreInfoDocument = (Document) coreInfoAndList.get(2);
    Document environmentIdentifierCoreInfoElemMatchDocument =
        (Document) environmentIdentifierCoreInfoDocument.get(AuditEventKeys.coreInfo);
    Document elemMatchEnvironmentIdentifierDocument =
        (Document) environmentIdentifierCoreInfoElemMatchDocument.get(MONGO_ELEM_MATCH_OPERATOR);
    assertEquals(AuditCommonConstants.ENVIRONMENT_IDENTIFIER,
        elemMatchEnvironmentIdentifierDocument.getString(KeyValuePairKeys.key));
    Document environmentIdentifierValueDocument =
        (Document) elemMatchEnvironmentIdentifierDocument.get(KeyValuePairKeys.value);
    List<String> environmentIdentifierInList = (List<String>) environmentIdentifierValueDocument.get(MONGO_IN_OPERATOR);
    assertEquals(1, environmentIdentifierInList.size());
    assertEquals(environmentIdentifier, environmentIdentifierInList.get(0));
  }
}
