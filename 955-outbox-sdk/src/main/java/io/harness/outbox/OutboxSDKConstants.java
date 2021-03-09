package io.harness.outbox;

import static io.harness.beans.SortOrder.OrderType.ASC;

import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.outbox.OutboxEvent.OutboxEventKeys;

import java.util.Collections;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OutboxSDKConstants {
  public static final String OUTBOX_TRANSACTION_TEMPLATE = "OUTBOX_TRANSACTION_TEMPLATE";
  public static final String OUTBOX_POLL_JOB_PAGE_REQUEST = "OUTBOX_POLL_JOB_PAGE_REQUEST";

  public static final PageRequest DEFAULT_OUTBOX_POLL_PAGE_REQUEST =
      PageRequest.builder()
          .pageIndex(0)
          .pageSize(20)
          .sortOrders(Collections.singletonList(
              SortOrder.Builder.aSortOrder().withField(OutboxEventKeys.createdAt, ASC).build()))
          .build();
}
