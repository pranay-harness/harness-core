package io.harness.outbox.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.SortOrder.OrderType.ASC;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_OUTBOX_POLL_PAGE_REQUEST;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.utils.PageTestUtils.getPage;
import static io.harness.utils.PageUtils.getNGPageResponse;

import static java.util.Collections.emptyList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.event.Event;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.OutboxEvent.OutboxEventKeys;
import io.harness.outbox.api.OutboxDao;
import io.harness.rule.Owner;

import com.google.gson.Gson;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

@OwnedBy(PL)
public class OutboxServiceImplTest extends CategoryTest {
  private OutboxDao outboxDao;
  private OutboxServiceImpl outboxService;

  @Before
  public void setup() {
    outboxDao = mock(OutboxDao.class);
    outboxService = spy(new OutboxServiceImpl(outboxDao, new Gson()));
  }

  @Data
  @Builder
  private static class SampleEvent implements Event {
    @NotNull ResourceScope resourceScope;
    @NotNull @Valid Resource resource;
    Object eventData;
    @NotEmpty String eventType;
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testSave() {
    String accountIdentifier = randomAlphabetic(10);
    SampleEvent event = SampleEvent.builder().resourceScope(new AccountScope(accountIdentifier)).build();
    ArgumentCaptor<OutboxEvent> outboxEventArgumentCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
    when(outboxDao.save(any(OutboxEvent.class))).thenReturn(null);

    outboxService.save(event);
    verify(outboxDao, times(1)).save(outboxEventArgumentCaptor.capture());
    OutboxEvent outboxEvent = outboxEventArgumentCaptor.getValue();
    assertNotNull(outboxEvent);
    assertNotNull(outboxEvent.getResourceScope());
    assertEquals("account", outboxEvent.getResourceScope().getScope());
    assertEquals(accountIdentifier, ((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList() {
    ArgumentCaptor<PageRequest> pageRequestArgumentCaptor = ArgumentCaptor.forClass(PageRequest.class);
    when(outboxDao.list(any(PageRequest.class))).thenReturn(getNGPageResponse(getPage(emptyList(), 0)));

    outboxService.list(null);
    verify(outboxDao, times(1)).list(pageRequestArgumentCaptor.capture());
    PageRequest pageRequest = pageRequestArgumentCaptor.getValue();
    assertNotNull(pageRequest);
    assertEquals(DEFAULT_OUTBOX_POLL_PAGE_REQUEST.getPageIndex(), pageRequest.getPageIndex());
    assertEquals(DEFAULT_OUTBOX_POLL_PAGE_REQUEST.getPageSize(), pageRequest.getPageSize());
    assertEquals(OutboxEventKeys.createdAt, pageRequest.getSortOrders().get(0).getFieldName());
    assertEquals(ASC, pageRequest.getSortOrders().get(0).getOrderType());
  }
}
