package io.harness.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.KARAN;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.outbox.api.OutboxService;
import io.harness.rule.Owner;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PersistenceIteratorFactory.class, OutboxEventIteratorConfiguration.class})
@PowerMockIgnore({"javax.security.*", "javax.crypto.*", "javax.net.*"})
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PL)
public class OutboxEventIteratorHandlerTest extends CategoryTest {
  @Mock PersistenceIteratorFactory persistenceIteratorFactory;
  @Mock MongoTemplate mongoTemplate;
  @Mock OutboxEventHandler outboxEventHandler;
  @Mock OutboxService outboxService;
  @Mock OutboxEventIteratorConfiguration config;
  @InjectMocks OutboxEventIteratorHandler outboxEventIteratorHandler;

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testHandle() {
    String id = randomAlphabetic(10);
    OutboxEvent outboxEvent = OutboxEvent.builder().attempts(0L).blocked(false).id(id).build();
    when(outboxEventHandler.handle(outboxEvent)).thenReturn(true);
    when(config.getMaximumOutboxEventHandlingAttempts()).thenReturn(10L);
    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    when(outboxService.delete(any())).thenReturn(true);
    outboxEventIteratorHandler.handle(outboxEvent);
    verify(outboxService, times(1)).delete(stringArgumentCaptor.capture());
    String outboxId = stringArgumentCaptor.getValue();
    assertEquals(id, outboxId);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testMaxAttemptsHandle() {
    String id = randomAlphabetic(10);
    OutboxEvent outboxEvent = OutboxEvent.builder().attempts(9L).blocked(false).id(id).build();
    when(outboxEventHandler.handle(outboxEvent)).thenReturn(false);
    when(config.getMaximumOutboxEventHandlingAttempts()).thenReturn(10L);
    ArgumentCaptor<OutboxEvent> outboxEventArgumentCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
    when(outboxService.update(any(OutboxEvent.class))).thenReturn(null);
    outboxEventIteratorHandler.handle(outboxEvent);
    verify(outboxService, times(1)).update(outboxEventArgumentCaptor.capture());
    OutboxEvent outbox = outboxEventArgumentCaptor.getValue();
    assertTrue(outbox.getBlocked());
    assertEquals(10L, outbox.getAttempts().longValue());
  }
}
