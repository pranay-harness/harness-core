package io.harness.outbox;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OutboxSDKConstants {
  public static final String OUTBOX_TRANSACTION_TEMPLATE = "OUTBOX_TRANSACTION_TEMPLATE";
  public static final String OUTBOX_POLL_JOB_PAGE_REQUEST = "OUTBOX_POLL_JOB_PAGE_REQUEST";

  public static final int DEFAULT_OUTBOX_POLL_PAGE_SIZE = 20;
}
