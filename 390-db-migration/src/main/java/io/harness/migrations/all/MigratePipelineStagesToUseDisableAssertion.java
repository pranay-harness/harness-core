/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.dl.WingsPersistence;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MigratePipelineStagesToUseDisableAssertion implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  private static final String DEBUG_LINE = "[Pipeline Disable Migration]:";

  private static final String SET_DISABLE_ASSERTION = " Set disable assertion for ";

  private static final String WITH_ID = " with Id";

  @Override
  public void migrate() {
    log.info(String.join(DEBUG_LINE, " Starting Migration For Disable Assertion"));
    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        log.info(
            String.join(DEBUG_LINE, " Starting Migration For Disable Assertion for account", account.getAccountName()));
        migratePipelinesForAccount(account);
      }
    } catch (Exception ex) {
      log.info(String.join(DEBUG_LINE, " Exception while fetching Accounts"));
    }
  }

  private void migratePipelinesForAccount(Account account) {
    try (HIterator<Pipeline> pipelines = new HIterator<>(
             wingsPersistence.createQuery(Pipeline.class).filter(PipelineKeys.accountId, account.getUuid()).fetch())) {
      log.info(String.join(
          DEBUG_LINE, " Fetching Pipelines for account", account.getAccountName(), WITH_ID, account.getUuid()));
      while (pipelines.hasNext()) {
        migratePipeline(pipelines.next());
      }
    } catch (Exception ex) {
      log.info(String.join(DEBUG_LINE, " Exception while fetching pipelines with Account ", account.getUuid()));
    }
  }

  private void migratePipeline(Pipeline pipeline) {
    boolean modified = false;
    try {
      log.info(String.join(DEBUG_LINE, " Starting Migration for ", pipeline.getName(), WITH_ID, pipeline.getUuid()));
      for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
        for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
          if ((pipelineStageElement.getType().equals(StateType.APPROVAL.name())
                  || pipelineStageElement.getType().equals(StateType.ENV_STATE.name()))
              && pipelineStageElement.isDisable()) {
            pipelineStageElement.setDisableAssertion("true");
            log.info(String.join(DEBUG_LINE, SET_DISABLE_ASSERTION, pipelineStageElement.getName(), WITH_ID,
                pipelineStageElement.getUuid()));
            modified = true;
          }
        }
      }
      if (modified) {
        wingsPersistence.save(pipeline);
        log.info(String.join(DEBUG_LINE, SET_DISABLE_ASSERTION, pipeline.getName(), WITH_ID, pipeline.getUuid()));
      } else {
        log.info(String.join(
            DEBUG_LINE, " No disabled steps found for  pipeline ", pipeline.getName(), WITH_ID, pipeline.getUuid()));
      }

    } catch (RuntimeException e) {
      log.info(String.join(DEBUG_LINE, SET_DISABLE_ASSERTION, pipeline.getName(), WITH_ID, pipeline.getUuid(),
          "Failed With RuntimeException", e.getMessage()));
    } catch (Exception e) {
      log.info(String.join(DEBUG_LINE, SET_DISABLE_ASSERTION, pipeline.getName(), WITH_ID, pipeline.getUuid(),
          "Failed With Exception", e.getMessage()));
    }
  }
}
