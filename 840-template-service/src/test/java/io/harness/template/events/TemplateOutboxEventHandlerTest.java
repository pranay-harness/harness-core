package io.harness.template.events;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ARCHIT;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.client.api.AuditClientService;
import io.harness.category.element.UnitTests;
import io.harness.context.GlobalContext;
import io.harness.encryption.Scope;
import io.harness.outbox.OutboxEvent;
import io.harness.rule.Owner;
import io.harness.security.SourcePrincipalContextData;
import io.harness.security.dto.Principal;
import io.harness.security.dto.UserPrincipal;
import io.harness.template.entity.TemplateEntity;
import io.harness.utils.NGObjectMapperHelper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

@OwnedBy(CDC)
public class TemplateOutboxEventHandlerTest extends CategoryTest {
  private ObjectMapper objectMapper;
  private AuditClientService auditClientService;
  private TemplateOutboxEventHandler templateOutboxEventHandler;
  String newYaml;
  String oldYaml;

  @Before
  public void setup() throws IOException {
    objectMapper = NGObjectMapperHelper.NG_PIPELINE_OBJECT_MAPPER;
    auditClientService = mock(AuditClientService.class);
    templateOutboxEventHandler = spy(new TemplateOutboxEventHandler(auditClientService));
    newYaml = Resources.toString(this.getClass().getClassLoader().getResource("template.yaml"), Charsets.UTF_8);
    oldYaml = Resources.toString(this.getClass().getClassLoader().getResource("template_updated.yaml"), Charsets.UTF_8);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCreate() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String templateVersionLabel = randomAlphabetic(10);

    TemplateEntity templateEntity = TemplateEntity.builder()
                                        .name(randomAlphabetic(10))
                                        .identifier(identifier)
                                        .versionLabel(templateVersionLabel)
                                        .templateScope(Scope.PROJECT)
                                        .yaml(newYaml)
                                        .build();
    TemplateCreateEvent createEvent =
        new TemplateCreateEvent(accountIdentifier, orgIdentifier, projectIdentifier, templateEntity);
    String eventData = objectMapper.writeValueAsString(createEvent);
    GlobalContext globalContext = new GlobalContext();
    Principal principal =
        new UserPrincipal(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
    SourcePrincipalContextData sourcePrincipalContextData =
        SourcePrincipalContextData.builder().principal(principal).build();
    globalContext.upsertGlobalContextRecord(sourcePrincipalContextData);

    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .resource(createEvent.getResource())
                                  .resourceScope(createEvent.getResourceScope())
                                  .eventType(TemplateOutboxEvents.TEMPLATE_VERSION_CREATED)
                                  .blocked(false)
                                  .globalContext(globalContext)
                                  .createdAt(Long.valueOf(randomNumeric(6)))
                                  .eventData(eventData)
                                  .id(randomAlphabetic(10))
                                  .build();
    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any(), any())).thenReturn(true);
    templateOutboxEventHandler.handle(outboxEvent);
    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any(), any());

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertAuditEntry(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, templateVersionLabel, auditEntry, outboxEvent);
    assertEquals(Action.CREATE, auditEntry.getAction());
    assertNull(auditEntry.getOldYaml());
    assertEquals(newYaml, auditEntry.getNewYaml());
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testUpdate() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String templateVersionLabel = randomAlphabetic(10);

    TemplateEntity oldTemplateEntity = TemplateEntity.builder()
                                           .name(randomAlphabetic(10))
                                           .identifier(identifier)
                                           .versionLabel(templateVersionLabel)
                                           .templateScope(Scope.PROJECT)
                                           .yaml(oldYaml)
                                           .build();
    TemplateEntity newTemplateEntity = TemplateEntity.builder()
                                           .name(randomAlphabetic(10))
                                           .identifier(identifier)
                                           .versionLabel(templateVersionLabel)
                                           .templateScope(Scope.PROJECT)
                                           .yaml(newYaml)
                                           .build();
    TemplateUpdateEvent updateEvent = new TemplateUpdateEvent(
        accountIdentifier, orgIdentifier, projectIdentifier, newTemplateEntity, oldTemplateEntity);
    String eventData = objectMapper.writeValueAsString(updateEvent);
    GlobalContext globalContext = new GlobalContext();
    Principal principal =
        new UserPrincipal(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
    SourcePrincipalContextData sourcePrincipalContextData =
        SourcePrincipalContextData.builder().principal(principal).build();
    globalContext.upsertGlobalContextRecord(sourcePrincipalContextData);

    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .resource(updateEvent.getResource())
                                  .resourceScope(updateEvent.getResourceScope())
                                  .eventType(TemplateOutboxEvents.TEMPLATE_VERSION_UPDATED)
                                  .blocked(false)
                                  .globalContext(globalContext)
                                  .createdAt(Long.valueOf(randomNumeric(6)))
                                  .eventData(eventData)
                                  .id(randomAlphabetic(10))
                                  .build();
    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any(), any())).thenReturn(true);
    templateOutboxEventHandler.handle(outboxEvent);
    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any(), any());

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertAuditEntry(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, templateVersionLabel, auditEntry, outboxEvent);
    assertEquals(Action.UPDATE, auditEntry.getAction());
    assertEquals(newYaml, auditEntry.getNewYaml());
    assertEquals(oldYaml, auditEntry.getOldYaml());
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testDelete() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String templateVersionLabel = randomAlphabetic(10);

    TemplateEntity templateEntity = TemplateEntity.builder()
                                        .name(randomAlphabetic(10))
                                        .identifier(identifier)
                                        .versionLabel(templateVersionLabel)
                                        .templateScope(Scope.PROJECT)
                                        .yaml(oldYaml)
                                        .build();
    TemplateDeleteEvent deleteEvent =
        new TemplateDeleteEvent(accountIdentifier, orgIdentifier, projectIdentifier, templateEntity);
    String eventData = objectMapper.writeValueAsString(deleteEvent);
    GlobalContext globalContext = new GlobalContext();
    Principal principal =
        new UserPrincipal(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
    SourcePrincipalContextData sourcePrincipalContextData =
        SourcePrincipalContextData.builder().principal(principal).build();
    globalContext.upsertGlobalContextRecord(sourcePrincipalContextData);

    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .resource(deleteEvent.getResource())
                                  .resourceScope(deleteEvent.getResourceScope())
                                  .eventType(TemplateOutboxEvents.TEMPLATE_VERSION_DELETED)
                                  .blocked(false)
                                  .globalContext(globalContext)
                                  .createdAt(Long.valueOf(randomNumeric(6)))
                                  .eventData(eventData)
                                  .id(randomAlphabetic(10))
                                  .build();
    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any(), any())).thenReturn(true);
    templateOutboxEventHandler.handle(outboxEvent);
    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any(), any());

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertAuditEntry(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, templateVersionLabel, auditEntry, outboxEvent);
    assertEquals(Action.DELETE, auditEntry.getAction());
    assertNull(auditEntry.getNewYaml());
    assertEquals(oldYaml, auditEntry.getOldYaml());
  }

  private void assertAuditEntry(String accountId, String orgIdentifier, String projectIdentifier, String identifier,
      String templateVersionLabel, AuditEntry auditEntry, OutboxEvent outboxEvent) {
    assertNotNull(auditEntry);
    assertEquals(accountId, auditEntry.getResourceScope().getAccountIdentifier());
    assertEquals(orgIdentifier, auditEntry.getResourceScope().getOrgIdentifier());
    assertEquals(projectIdentifier, auditEntry.getResourceScope().getProjectIdentifier());
    assertEquals(auditEntry.getInsertId(), outboxEvent.getId());
    assertEquals(identifier, auditEntry.getResource().getIdentifier());
    assertEquals(templateVersionLabel, auditEntry.getResource().getLabels().get("versionLabel"));
    assertEquals(ModuleType.TEMPLATESERVICE, auditEntry.getModule());
    assertEquals(outboxEvent.getCreatedAt().longValue(), auditEntry.getTimestamp());
  }
}