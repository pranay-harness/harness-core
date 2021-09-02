package io.harness.springdata;

import com.google.inject.Inject;
import com.mongodb.MongoException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.data.mongodb.MongoTransactionException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class TransactionHelper {
  private final RetryPolicy<Object> transactionRetryPolicy =
      new RetryPolicy<>()
          .withDelay(Duration.ofSeconds(1))
          .withMaxAttempts(3)
          .onFailedAttempt(event
              -> log.info("Retrying Transaction. Attempt No. {}", event.getAttemptCount(), event.getLastFailure()))
          .onFailure(event -> log.error("Transaction Failed", event.getFailure()))
          .handle(TransactionException.class)
          .handle(MongoException.class)
          .handle(MongoTransactionException.class)
          .handle(UncategorizedMongoDbException.class);

  @Inject private TransactionTemplate transactionTemplate;

  public <T> T performTransaction(TransactionFunction<T> transactionFunction) {
    return Failsafe.with(transactionRetryPolicy)
        .get(() -> transactionTemplate.execute(t -> transactionFunction.execute()));
  }

  public interface TransactionFunction<R> {
    R execute();
  }
}
