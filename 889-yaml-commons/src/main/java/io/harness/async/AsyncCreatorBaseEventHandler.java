/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.async;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.execution.events.PmsCommonsBaseEventHandler;
import io.harness.pms.yaml.YamlField;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public abstract class AsyncCreatorBaseEventHandler<T extends Message, C extends AsyncCreatorContext>
    implements PmsCommonsBaseEventHandler<T> {
  @Inject public PmsGitSyncHelper pmsGitSyncHelper;
  @Inject public ExceptionManager exceptionManager;

  @NonNull protected abstract Map<String, String> extraLogProperties(T event);

  protected abstract Map<String, YamlFieldBlob> extractDependencies(T message);

  protected abstract C extractContext(T message);

  @Override
  public void handleEvent(T event, Map<String, String> metadataMap, long createdAt) {
    try {
      Map<String, YamlFieldBlob> dependencyBlobs = extractDependencies(event);
      Map<String, YamlField> initialDependencies = new HashMap<>();
      if (EmptyPredicate.isNotEmpty(dependencyBlobs)) {
        try {
          for (Map.Entry<String, YamlFieldBlob> entry : dependencyBlobs.entrySet()) {
            initialDependencies.put(entry.getKey(), YamlField.fromFieldBlob(entry.getValue()));
          }
        } catch (Exception e) {
          log.error("Invalid YAML found in dependency blobs", e);
          throw new InvalidRequestException("Invalid YAML found in dependency blobs", e);
        }
      }
      AsyncCreatorResponse finalResponse = handleDependenciesRecursive(initialDependencies, extractContext(event));
      handleResult(event, finalResponse);
    } catch (Exception ex) {
      log.error(ExceptionUtils.getMessage(ex), ex);
      handleException(event, ex);
    }
  }

  protected abstract void handleResult(T event, AsyncCreatorResponse creatorResponse);

  private AsyncCreatorResponse handleDependenciesRecursive(Map<String, YamlField> initialDependencies, C context) {
    // TODO: Add patch version before sending the response back
    AsyncCreatorResponse finalResponse = createNewAsyncCreatorResponse();
    if (EmptyPredicate.isEmpty(initialDependencies)) {
      return finalResponse;
    }

    ByteString gitSyncBranchContext = context.getGitSyncBranchContext();

    try (PmsGitSyncBranchContextGuard ignore =
             pmsGitSyncHelper.createGitSyncBranchContextGuardFromBytes(gitSyncBranchContext, true)) {
      Map<String, YamlField> dependencies = new HashMap<>(initialDependencies);
      while (!dependencies.isEmpty()) {
        handleDependencies(context, finalResponse, dependencies);
        initialDependencies.keySet().forEach(dependencies::remove);
      }
    }

    if (EmptyPredicate.isNotEmpty(finalResponse.getDependencies().getDependenciesMap())) {
      initialDependencies.keySet().forEach(k -> finalResponse.getDependencies().getDependenciesMap().remove(k));
    }
    return finalResponse;
  }

  protected abstract AsyncCreatorResponse createNewAsyncCreatorResponse();

  public abstract void handleDependencies(
      C ctx, AsyncCreatorResponse finalResponse, Map<String, YamlField> dependencies);

  protected abstract void handleException(T event, YamlField field, Exception ex);
  protected abstract void handleException(T event, Exception ex);
}
