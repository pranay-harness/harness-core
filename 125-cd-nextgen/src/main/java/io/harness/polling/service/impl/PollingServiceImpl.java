/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.polling.service.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.observer.Subject;
import io.harness.polling.bean.PolledResponse;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.bean.PollingDocument.PollingDocumentKeys;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.mapper.PollingDocumentMapper;
import io.harness.polling.service.intfc.PollingService;
import io.harness.polling.service.intfc.PollingServiceObserver;
import io.harness.repositories.polling.PollingRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
@Singleton
public class PollingServiceImpl implements PollingService {
  @Inject private PollingRepository pollingRepository;
  @Inject private PollingDocumentMapper pollingDocumentMapper;
  @Inject @Getter private final Subject<PollingServiceObserver> subject = new Subject<>();

  @Override
  public String save(PollingDocument pollingDocument) {
    validatePollingDocument(pollingDocument);
    PollingDocument savedPollingDoc = pollingRepository.addSubscribersToExistingPollingDoc(
        pollingDocument.getAccountId(), pollingDocument.getOrgIdentifier(), pollingDocument.getProjectIdentifier(),
        pollingDocument.getPollingType(), pollingDocument.getPollingInfo(), pollingDocument.getSignatures());
    // savedPollingDoc will be null if we couldn't find polling doc with the same entries as pollingDocument.
    if (savedPollingDoc == null) {
      savedPollingDoc = pollingRepository.save(pollingDocument);
      createPerpetualTask(savedPollingDoc);
    }
    return savedPollingDoc.getUuid();
  }

  private void validatePollingDocument(PollingDocument pollingDocument) {
    if (EmptyPredicate.isEmpty(pollingDocument.getAccountId())) {
      throw new InvalidRequestException("AccountId should not be empty");
    }
    if (EmptyPredicate.isEmpty(pollingDocument.getSignatures())) {
      throw new InvalidRequestException("Signature should not be empty");
    }
  }

  @Override
  public PollingDocument get(String accountId, String pollingDocId) {
    return pollingRepository.findByUuidAndAccountId(pollingDocId, accountId);
  }

  @Override
  public void delete(PollingDocument pollingDocument) {
    PollingDocument savedPollDoc = pollingRepository.removeDocumentIfOnlySubscriber(
        pollingDocument.getAccountId(), pollingDocument.getUuid(), pollingDocument.getSignatures());
    // if savedPollDoc is null that means either it was not the only subscriber or this poll doc doesn't exist in db.
    if (savedPollDoc == null) {
      pollingRepository.removeSubscribersFromExistingPollingDoc(
          pollingDocument.getAccountId(), pollingDocument.getUuid(), pollingDocument.getSignatures());
    } else {
      deletePerpetualTask(savedPollDoc);
    }
  }

  @Override
  public boolean attachPerpetualTask(String accountId, String pollDocId, String perpetualTaskId) {
    UpdateResult updateResult = pollingRepository.updateSelectiveEntity(
        accountId, pollDocId, PollingDocumentKeys.perpetualTaskId, perpetualTaskId);
    return updateResult.getModifiedCount() != 0;
  }

  @Override
  public void updateFailedAttempts(String accountId, String pollingDocId, int failedAttempts) {
    pollingRepository.updateSelectiveEntity(
        accountId, pollingDocId, PollingDocumentKeys.failedAttempts, failedAttempts);
  }

  @Override
  public void updatePolledResponse(String accountId, String pollingDocId, PolledResponse polledResponse) {
    pollingRepository.updateSelectiveEntity(
        accountId, pollingDocId, PollingDocumentKeys.polledResponse, polledResponse);
  }

  @Override
  public String subscribe(PollingItem pollingItem) throws InvalidRequestException {
    PollingDocument pollingDocument = pollingDocumentMapper.toPollingDocument(pollingItem);
    PollingDocument existingPollingDoc = null;
    if (pollingDocument.getUuid() != null) {
      existingPollingDoc = pollingRepository.findByUuidAndAccountIdAndSignature(
          pollingDocument.getUuid(), pollingDocument.getAccountId(), pollingDocument.getSignatures());
    }

    // Determine if update request
    if (existingPollingDoc == null) {
      return save(pollingDocument);
    }

    if (existingPollingDoc.getPollingInfo().equals(pollingDocument.getPollingInfo())) {
      return existingPollingDoc.getUuid();
    } else {
      delete(pollingDocument);
      // Note: This is intentional. The pollingDocId sent to us is stale, we need to set it to null so that the save
      // call creates a new pollingDoc
      pollingDocument.setUuid(null);
      return save(pollingDocument);
    }
  }

  @Override
  public boolean unsubscribe(PollingItem pollingItem) {
    PollingDocument pollingDocument = pollingDocumentMapper.toPollingDocument(pollingItem);
    delete(pollingDocument);
    return true;
  }

  private void createPerpetualTask(@NotNull PollingDocument pollingDocument) {
    try {
      subject.fireInform(PollingServiceObxxxxxxxx:onSaved, pollingDocument);
    } catch (Exception e) {
      log.error("Encountered exception while informing the observers of Polling Document on save for polling doc: {}",
          pollingDocument.getUuid(), e);
    }
  }

  // TODO: Do not delete. Tihs will be used for connector update case.
  private void resetPerpetualTask(@NotNull PollingDocument pollingDocument) {
    try {
      subject.fireInform(PollingServiceObxxxxxxxx:onUpdated, pollingDocument);
    } catch (Exception e) {
      log.error("Encountered exception while informing the observers of Polling Document on update for polling doc: {}",
          pollingDocument.getUuid(), e);
    }
  }

  private void deletePerpetualTask(@NotNull PollingDocument pollingDocument) {
    try {
      subject.fireInform(PollingServiceObxxxxxxxx:onDeleted, pollingDocument);
    } catch (Exception e) {
      log.error("Encountered exception while informing the observers of Polling Document on delete for polling doc: {}",
          pollingDocument.getUuid(), e);
    }
  }
}
