package io.harness.outbox;

import static io.harness.beans.SortOrder.OrderType.ASC;

import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.outbox.OutboxEvent.OutboxEventKeys;

import java.util.Collections;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OutboxSDKConstants {
  public static final PageRequest DEFAULT_OUTBOX_POLL_PAGE_REQUEST =
      PageRequest.builder()
          .pageIndex(0)
          .pageSize(20)
          .sortOrders(Collections.singletonList(
              SortOrder.Builder.aSortOrder().withField(OutboxEventKeys.createdAt, ASC).build()))
          .build();
}
