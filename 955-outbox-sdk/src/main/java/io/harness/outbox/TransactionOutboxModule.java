package io.harness.outbox;

import static io.harness.beans.SortOrder.OrderType.ASC;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_OUTBOX_POLL_PAGE_SIZE;
import static io.harness.outbox.OutboxSDKConstants.OUTBOX_POLL_JOB_PAGE_REQUEST;
import static io.harness.outbox.OutboxSDKConstants.OUTBOX_TRANSACTION_TEMPLATE;

import io.harness.beans.SortOrder;
import io.harness.mongo.MongoConfig;
import io.harness.ng.beans.PageRequest;
import io.harness.outbox.OutboxEvent.OutboxEventKeys;
import io.harness.outbox.api.OutboxEventService;
import io.harness.outbox.api.impl.OutboxEventServiceImpl;
import io.harness.springdata.HTransactionTemplate;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class TransactionOutboxModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(OutboxEventService.class).to(OutboxEventServiceImpl.class);
  }

  @Provides
  @Singleton
  @Named(OUTBOX_TRANSACTION_TEMPLATE)
  protected TransactionTemplate getTransactionTemplate(
      MongoTransactionManager mongoTransactionManager, MongoConfig mongoConfig) {
    return new HTransactionTemplate(mongoTransactionManager, mongoConfig.isTransactionsEnabled());
  }

  @Provides
  @Singleton
  @Named(OUTBOX_POLL_JOB_PAGE_REQUEST)
  protected PageRequest getPageRequest() {
    List<SortOrder> sortOrders = new ArrayList<>();
    sortOrders.add(SortOrder.Builder.aSortOrder().withField(OutboxEventKeys.createdAt, ASC).build());
    return PageRequest.builder().pageIndex(0).pageSize(DEFAULT_OUTBOX_POLL_PAGE_SIZE).sortOrders(sortOrders).build();
  }
}
