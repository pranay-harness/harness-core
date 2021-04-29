package io.harness.engine.utils;

import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class TransactionUtils {
  private final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}",
      "[Failed] attempt: {}", ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject private TransactionTemplate transactionTemplate;

  public <T> T performTransaction(TransactionFunction<T> transactionFunction) {
    return Failsafe.with(transactionRetryPolicy)
        .get(() -> transactionTemplate.execute(t -> transactionFunction.execute()));
  }

  public interface TransactionFunction<R> {
    R execute();
  }
}
