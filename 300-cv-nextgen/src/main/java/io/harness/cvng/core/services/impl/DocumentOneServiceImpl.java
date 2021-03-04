package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.services.api.DocumentOneService;
import io.harness.exception.UnexpectedException;
import io.harness.repositories.DocumentOne;
import io.harness.repositories.repo.DocumentOneRepo;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class DocumentOneServiceImpl implements DocumentOneService {
  @Inject DocumentOneRepo documentOneRepo;
  @Inject TransactionTemplate transactionTemplate;

  private static final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy(
      "[Retrying]: Failed to add user; attempt: {}", "[Failed]: Failed to add user with amount > 5000; attempt: {}",
      ImmutableList.of(TransactionException.class), Duration.ofSeconds(2), 3, log);

  @Override
  @Transactional
  public void save(DocumentOne documentOne) {
    Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      DocumentOne one = documentOneRepo.save(documentOne);
      if (one.getAmount() > 5000) {
        throw new UnexpectedException("amount cannot be more than 5000");
      }
      return one;
    }));
  }

  @Override
  public List<DocumentOne> get() {
    return (List<DocumentOne>) documentOneRepo.findAll();
  }
}
