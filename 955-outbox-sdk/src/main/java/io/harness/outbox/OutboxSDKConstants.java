package io.harness.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.SortOrder.OrderType.ASC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.outbox.OutboxEvent.OutboxEventKeys;

import java.util.Collections;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class OutboxSDKConstants {
  public static final PageRequest DEFAULT_OUTBOX_POLL_PAGE_REQUEST =
      PageRequest.builder()
          .pageIndex(0)
          .pageSize(10)
          .sortOrders(Collections.singletonList(
              SortOrder.Builder.aSortOrder().withField(OutboxEventKeys.createdAt, ASC).build()))
          .build();

  public static final long DEFAULT_MAX_ATTEMPTS = 10;

  public static final OutboxEventIteratorConfiguration DEFAULT_OUTBOX_ITERATOR_CONFIGURATION =
      OutboxEventIteratorConfiguration.builder()
          .threadPoolSize(2)
          .intervalInSeconds(90)
          .targetIntervalInSeconds(60)
          .acceptableNoAlertDelayInSeconds(60)
          .maximumOutboxEventHandlingAttempts(10)
          .build();
}
