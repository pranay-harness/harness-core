package io.harness.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.SortOrder.OrderType.ASC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.outbox.OutboxEvent.OutboxEventKeys;
import io.harness.outbox.filter.OutboxEventFilter;

import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class OutboxSDKConstants {
  public static final List<SortOrder> DEFAULT_CREATED_AT_ASC_SORT_ORDER =
      Collections.singletonList(SortOrder.Builder.aSortOrder().withField(OutboxEventKeys.createdAt, ASC).build());

  public static final int DEFAULT_MAX_ATTEMPTS = 10;

  public static final int DEFAULT_MAX_EVENTS_POLLED = 50;

  public static final OutboxEventIteratorConfiguration DEFAULT_OUTBOX_ITERATOR_CONFIGURATION =
      OutboxEventIteratorConfiguration.builder()
          .threadPoolSize(3)
          .intervalInSeconds(5)
          .targetIntervalInSeconds(15)
          .acceptableNoAlertDelayInSeconds(60)
          .maximumOutboxEventHandlingAttempts(10)
          .build();

  public static final OutboxPollConfiguration DEFAULT_OUTBOX_POLL_CONFIGURATION =
      OutboxPollConfiguration.builder()
          .maximumRetryAttemptsForAnEvent(DEFAULT_MAX_ATTEMPTS)
          .initialDelayInSeconds(5)
          .pollingIntervalInSeconds(5)
          .build();

  public static final OutboxEventFilter DEFAULT_OUTBOX_EVENT_FILTER =
      OutboxEventFilter.builder()
          .maximumEventsPolled(DEFAULT_MAX_EVENTS_POLLED)
          .blocked(false)
          .maximumAttempts(DEFAULT_MAX_ATTEMPTS)
          .build();
}
