package io.harness.ng.core.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.remote.NGObjectMapperHelper.NG_DEFAULT_OBJECT_MAPPER;
import static io.harness.rule.OwnerRule.KARAN;

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
import io.harness.audit.ResourceTypeConstants;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.client.api.AuditClientService;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.api.Producer;
import io.harness.ng.core.events.UserInviteCreateEvent;
import io.harness.ng.core.events.UserInviteDeleteEvent;
import io.harness.ng.core.events.UserInviteUpdateEvent;
import io.harness.ng.core.invites.dto.InviteDTO;
import io.harness.ng.core.invites.remote.RoleBinding;
import io.harness.outbox.OutboxEvent;
import io.harness.rule.Owner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

@OwnedBy(PL)
public class UserEventHandlerTest extends CategoryTest {
  private ObjectMapper objectMapper;
  private Producer producer;
  private AuditClientService auditClientService;
  private UserEventHandler userEventHandler;

  @Before
  public void setup() {
    objectMapper = NG_DEFAULT_OBJECT_MAPPER;
    producer = mock(Producer.class);
    auditClientService = mock(AuditClientService.class);
    userEventHandler = spy(new UserEventHandler(producer, auditClientService));
  }

  private InviteDTO getInviteDTO(String email) {
    List<RoleBinding> roleBindings = new ArrayList<>();
    roleBindings.add(RoleBinding.builder().identifier(randomAlphabetic(10)).build());

    return InviteDTO.builder()
        .id(randomAlphabetic(10))
        .name(randomAlphabetic(10))
        .email(email)
        .roleBindings(roleBindings)
        .inviteType(null)
        .build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInviteCreate() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String email = randomAlphabetic(10);
    InviteDTO inviteDTO = getInviteDTO(email);
    UserInviteCreateEvent userInviteCreateEvent =
        new UserInviteCreateEvent(accountIdentifier, orgIdentifier, null, inviteDTO);
    String eventData = objectMapper.writeValueAsString(userInviteCreateEvent);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .id(randomAlphabetic(10))
                                  .blocked(false)
                                  .eventType("UserInviteCreated")
                                  .eventData(eventData)
                                  .resourceScope(userInviteCreateEvent.getResourceScope())
                                  .resource(userInviteCreateEvent.getResource())
                                  .createdAt(Long.parseLong(randomNumeric(5)))
                                  .build();

    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any())).thenReturn(true);

    userEventHandler.handle(outboxEvent);

    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any());

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertNotNull(auditEntry);
    assertEquals(outboxEvent.getId(), auditEntry.getInsertId());
    assertEquals(ResourceTypeConstants.USER, auditEntry.getResource().getType());
    assertEquals(email, auditEntry.getResource().getIdentifier());
    assertEquals(accountIdentifier, auditEntry.getResourceScope().getAccountIdentifier());
    assertEquals(orgIdentifier, auditEntry.getResourceScope().getOrgIdentifier());
    assertNull(auditEntry.getResourceScope().getProjectIdentifier());
    assertEquals(Action.INVITE, auditEntry.getAction());
    assertEquals(ModuleType.CORE, auditEntry.getModule());
    assertEquals(outboxEvent.getCreatedAt().longValue(), auditEntry.getTimestamp());
    assertNull(auditEntry.getEnvironment());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInviteUpdate() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String email = randomAlphabetic(10);
    InviteDTO oldInviteDTO = getInviteDTO(email);
    InviteDTO newInviteDTO = getInviteDTO(email);
    UserInviteUpdateEvent userInviteUpdateEvent =
        new UserInviteUpdateEvent(accountIdentifier, orgIdentifier, null, oldInviteDTO, newInviteDTO);
    String eventData = objectMapper.writeValueAsString(userInviteUpdateEvent);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .id(randomAlphabetic(10))
                                  .blocked(false)
                                  .eventType("UserInviteUpdated")
                                  .eventData(eventData)
                                  .resourceScope(userInviteUpdateEvent.getResourceScope())
                                  .resource(userInviteUpdateEvent.getResource())
                                  .createdAt(Long.parseLong(randomNumeric(5)))
                                  .build();

    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any())).thenReturn(true);

    userEventHandler.handle(outboxEvent);

    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any());

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertNotNull(auditEntry);
    assertEquals(outboxEvent.getId(), auditEntry.getInsertId());
    assertEquals(ResourceTypeConstants.USER, auditEntry.getResource().getType());
    assertEquals(email, auditEntry.getResource().getIdentifier());
    assertEquals(accountIdentifier, auditEntry.getResourceScope().getAccountIdentifier());
    assertEquals(orgIdentifier, auditEntry.getResourceScope().getOrgIdentifier());
    assertNull(auditEntry.getResourceScope().getProjectIdentifier());
    assertEquals(Action.RESEND_INVITE, auditEntry.getAction());
    assertEquals(ModuleType.CORE, auditEntry.getModule());
    assertEquals(outboxEvent.getCreatedAt().longValue(), auditEntry.getTimestamp());
    assertNull(auditEntry.getEnvironment());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testInviteDelete() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String email = randomAlphabetic(10);
    InviteDTO inviteDTO = getInviteDTO(email);
    UserInviteDeleteEvent userInviteDeleteEvent =
        new UserInviteDeleteEvent(accountIdentifier, orgIdentifier, null, inviteDTO);
    String eventData = objectMapper.writeValueAsString(userInviteDeleteEvent);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .id(randomAlphabetic(10))
                                  .blocked(false)
                                  .eventType("UserInviteDeleted")
                                  .eventData(eventData)
                                  .resourceScope(userInviteDeleteEvent.getResourceScope())
                                  .resource(userInviteDeleteEvent.getResource())
                                  .createdAt(Long.parseLong(randomNumeric(5)))
                                  .build();

    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any())).thenReturn(true);

    userEventHandler.handle(outboxEvent);

    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any());

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertNotNull(auditEntry);
    assertEquals(outboxEvent.getId(), auditEntry.getInsertId());
    assertEquals(ResourceTypeConstants.USER, auditEntry.getResource().getType());
    assertEquals(email, auditEntry.getResource().getIdentifier());
    assertEquals(accountIdentifier, auditEntry.getResourceScope().getAccountIdentifier());
    assertEquals(orgIdentifier, auditEntry.getResourceScope().getOrgIdentifier());
    assertNull(auditEntry.getResourceScope().getProjectIdentifier());
    assertEquals(Action.REVOKE_INVITE, auditEntry.getAction());
    assertEquals(ModuleType.CORE, auditEntry.getModule());
    assertEquals(outboxEvent.getCreatedAt().longValue(), auditEntry.getTimestamp());
    assertNull(auditEntry.getEnvironment());
  }
}
