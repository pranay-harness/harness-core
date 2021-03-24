package io.harness.audit.api.impl;

import static io.harness.NGCommonEntityConstants.ENVIRONMENT_IDENTIFIER_KEY;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.utils.PageTestUtils.getPage;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditService;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.beans.Principal;
import io.harness.audit.beans.PrincipalType;
import io.harness.audit.entities.AuditEvent;
import io.harness.audit.entities.AuditEvent.AuditEventKeys;
import io.harness.audit.repositories.AuditRepository;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.Resource;
import io.harness.ng.core.common.beans.KeyValuePair;
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
  private AuditService auditService;
  private final PageRequest samplePageRequest = PageRequest.builder().pageIndex(0).pageSize(50).build();

  @Before
  public void setup() {
    auditRepository = mock(AuditRepository.class);
    auditService = spy(new AuditServiceImpl(auditRepository));
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
    BasicDBList andList = (BasicDBList) criteria.getCriteriaObject().get("$and");
    assertNotNull(andList);
    assertEquals(2, andList.size());
    Document accountDocument = (Document) andList.get(0);
    assertEquals(accountIdentifier, accountDocument.getString(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));

    Document scopeDocument = (Document) andList.get(1);
    BasicDBList orList = (BasicDBList) scopeDocument.get("$or");
    assertNotNull(orList);
    Document accountOrgScopeDocument = (Document) orList.get(0);
    assertEquals(2, accountOrgScopeDocument.size());
    assertEquals(accountIdentifier, accountOrgScopeDocument.get(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));
    assertEquals(orgIdentifier, accountOrgScopeDocument.get(AuditEventKeys.ORG_IDENTIFIER_KEY));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInvalidScopeAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String randomValue = randomAlphabetic(10);
    AuditFilterPropertiesDTO invalidScopeFilter =
        AuditFilterPropertiesDTO.builder()
            .scopes(singletonList(ResourceScope.builder().accountIdentifier(accountIdentifier + "K").build()))
            .build();
    try {
      auditService.list(accountIdentifier, samplePageRequest, invalidScopeFilter);
      fail();
    } catch (InvalidRequestException exception) {
      // continue
    }
    AuditFilterPropertiesDTO invalidScopeLabelsFilter =
        AuditFilterPropertiesDTO.builder()
            .scopes(singletonList(ResourceScope.builder()
                                      .accountIdentifier(accountIdentifier)
                                      .labels(singletonList(KeyValuePair.builder().key("").value(randomValue).build()))
                                      .build()))
            .build();
    try {
      auditService.list(accountIdentifier, samplePageRequest, invalidScopeLabelsFilter);
      fail();
    } catch (InvalidRequestException exception) {
      // continue
    }
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
    BasicDBList andList = (BasicDBList) criteria.getCriteriaObject().get("$and");
    assertNotNull(andList);
    assertEquals(2, andList.size());
    Document accountDocument = (Document) andList.get(0);
    assertEquals(accountIdentifier, accountDocument.getString(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));

    Document resourceDocument = (Document) andList.get(1);
    BasicDBList orList = (BasicDBList) resourceDocument.get("$or");
    assertNotNull(orList);
    Document resourceTypeIdentifierScopeDocument = (Document) orList.get(0);
    assertEquals(2, resourceTypeIdentifierScopeDocument.size());
    assertEquals(resourceType, resourceTypeIdentifierScopeDocument.get(AuditEventKeys.RESOURCE_TYPE_KEY));
    assertEquals(identifier, resourceTypeIdentifierScopeDocument.get(AuditEventKeys.RESOURCE_IDENTIFIER_KEY));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInvalidResourceAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String resourceType = randomAlphabetic(10);
    String randomValue = randomAlphabetic(10);
    AuditFilterPropertiesDTO invalidResourceFilter =
        AuditFilterPropertiesDTO.builder()
            .resources(singletonList(Resource.builder().identifier(identifier).build()))
            .build();
    try {
      auditService.list(accountIdentifier, samplePageRequest, invalidResourceFilter);
      fail();
    } catch (InvalidRequestException exception) {
      // continue
    }
    AuditFilterPropertiesDTO invalidResourceLabelsFilter =
        AuditFilterPropertiesDTO.builder()
            .resources(
                singletonList(Resource.builder()
                                  .identifier(identifier)
                                  .type(resourceType)
                                  .labels(singletonList(KeyValuePair.builder().key("").value(randomValue).build()))
                                  .build()))
            .build();
    try {
      auditService.list(accountIdentifier, samplePageRequest, invalidResourceLabelsFilter);
      fail();
    } catch (InvalidRequestException exception) {
      // continue
    }
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
    BasicDBList andList = (BasicDBList) criteria.getCriteriaObject().get("$and");
    assertNotNull(andList);
    assertEquals(2, andList.size());
    Document accountDocument = (Document) andList.get(0);
    assertEquals(accountIdentifier, accountDocument.getString(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));

    Document principalDocument = (Document) andList.get(1);
    BasicDBList orList = (BasicDBList) principalDocument.get("$or");
    assertNotNull(orList);
    Document principalTypeIdentifierScopeDocument = (Document) orList.get(0);
    assertEquals(2, principalTypeIdentifierScopeDocument.size());
    assertEquals(principalType, principalTypeIdentifierScopeDocument.get(AuditEventKeys.PRINCIPAL_TYPE_KEY));
    assertEquals(identifier, principalTypeIdentifierScopeDocument.get(AuditEventKeys.PRINCIPAL_IDENTIFIER_KEY));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInvalidPrincipalAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    AuditFilterPropertiesDTO invalidPrincipalFilter =
        AuditFilterPropertiesDTO.builder()
            .principals(singletonList(Principal.builder().identifier(identifier).build()))
            .build();
    try {
      auditService.list(accountIdentifier, samplePageRequest, invalidPrincipalFilter);
      fail();
    } catch (InvalidRequestException exception) {
      // continue
    }
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
    BasicDBList andList = (BasicDBList) criteria.getCriteriaObject().get("$and");
    assertNotNull(andList);
    assertEquals(3, andList.size());
    Document accountDocument = (Document) andList.get(0);
    assertEquals(accountIdentifier, accountDocument.getString(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));

    Document startTimeDocument = (Document) andList.get(1);
    assertNotNull(startTimeDocument);
    Document startTimestampDocument = (Document) startTimeDocument.get(AuditEventKeys.timestamp);
    assertNotNull(startTimestampDocument);
    assertEquals(17L, startTimestampDocument.get("$gte"));

    Document endTimeDocument = (Document) andList.get(2);
    assertNotNull(endTimeDocument);
    Document endTimestampDocument = (Document) endTimeDocument.get(AuditEventKeys.timestamp);
    assertEquals(18L, endTimestampDocument.get("$lte"));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInvalidTimeAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    AuditFilterPropertiesDTO invalidTimeFilter = AuditFilterPropertiesDTO.builder().startTime(18L).endTime(17L).build();
    try {
      auditService.list(accountIdentifier, samplePageRequest, invalidTimeFilter);
      fail();
    } catch (InvalidRequestException exception) {
      // continue
    }
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testModuleTypeActionAndEnvironmentIdentifierAuditFilter() {
    String accountIdentifier = randomAlphabetic(10);
    ModuleType moduleType = ModuleType.CD;
    String action = randomAlphabetic(10);
    String environmentIdentifier = randomAlphabetic(10);
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(auditRepository.findAll(any(Criteria.class), any(Pageable.class))).thenReturn(getPage(emptyList(), 0));
    AuditFilterPropertiesDTO correctFilter = AuditFilterPropertiesDTO.builder()
                                                 .moduleTypes(singletonList(ModuleType.CD))
                                                 .actions(singletonList(action))
                                                 .environmentIdentifiers(singletonList(environmentIdentifier))
                                                 .build();
    Page<AuditEvent> auditEvents = auditService.list(accountIdentifier, samplePageRequest, correctFilter);
    verify(auditRepository, times(1)).findAll(criteriaArgumentCaptor.capture(), any(Pageable.class));
    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertNotNull(auditEvents);
    assertNotNull(criteria);
    BasicDBList andList = (BasicDBList) criteria.getCriteriaObject().get("$and");
    assertNotNull(andList);
    assertEquals(4, andList.size());
    Document accountDocument = (Document) andList.get(0);
    assertEquals(accountIdentifier, accountDocument.getString(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY));

    Document moduleTypeDocument = (Document) andList.get(1);
    assertNotNull(moduleTypeDocument);
    Document moduleTypeListDocument = (Document) moduleTypeDocument.get(AuditEventKeys.moduleType);
    assertNotNull(moduleTypeListDocument);
    List<ModuleType> moduleTypeList = (List<ModuleType>) moduleTypeListDocument.get("$in");
    assertEquals(1, moduleTypeList.size());
    assertEquals(moduleType, moduleTypeList.get(0));

    Document actionDocument = (Document) andList.get(2);
    assertNotNull(actionDocument);
    Document actionListDocument = (Document) actionDocument.get(AuditEventKeys.action);
    assertNotNull(actionListDocument);
    List<String> actionList = (List<String>) actionListDocument.get("$in");
    assertEquals(1, actionList.size());
    assertEquals(action, actionList.get(0));

    Document environmentIdentifierDocument = (Document) andList.get(3);
    assertNotNull(environmentIdentifierDocument);
    String environmentIdentifierKey = environmentIdentifierDocument.getString(AuditEventKeys.RESOURCE_LABEL_KEYS_KEY);
    assertNotNull(environmentIdentifierKey);
    assertEquals(ENVIRONMENT_IDENTIFIER_KEY, environmentIdentifierKey);
    Document environmentIdentifierValueDocument =
        (Document) environmentIdentifierDocument.get(AuditEventKeys.RESOURCE_LABEL_VALUES_KEY);
    List<String> environmentIdentifierValueList = (List<String>) environmentIdentifierValueDocument.get("$in");
    assertEquals(1, environmentIdentifierValueList.size());
    assertEquals(environmentIdentifier, environmentIdentifierValueList.get(0));
  }
}
