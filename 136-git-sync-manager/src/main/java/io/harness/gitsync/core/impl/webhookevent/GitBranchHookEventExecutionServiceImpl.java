/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.impl.webhookevent;

import static io.harness.gitsync.common.WebhookEventConstants.GIT_BRANCH_HOOK_EVENT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.gitsync.common.beans.BranchSyncStatus;
import io.harness.gitsync.common.beans.GitBranch;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.service.webhookevent.GitBranchHookEventExecutionService;
import io.harness.product.ci.scm.proto.Action;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.Repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.DeleteResult;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.DX)
@Singleton
@Slf4j
public class GitBranchHookEventExecutionServiceImpl implements GitBranchHookEventExecutionService {
  @Inject YamlGitConfigService yamlGitConfigService;
  @Inject GitBranchService gitBranchService;

  @Override
  public void processEvent(WebhookDTO webhookDTO) {
    try {
      ParseWebhookResponse scmParsedWebhookResponse = webhookDTO.getParsedResponse();
      if (scmParsedWebhookResponse == null || scmParsedWebhookResponse.getBranch() == null) {
        log.error("{} : Error while consuming webhook Parsed response : {}", GIT_BRANCH_HOOK_EVENT, webhookDTO);
        return;
      }
      final Action action = scmParsedWebhookResponse.getBranch().getAction();
      log.info("Received event for action {}", action);

      Repository repository = scmParsedWebhookResponse.getBranch().getRepo();

      // If repo doesn't exist, ignore the event
      if (Boolean.FALSE.equals(yamlGitConfigService.isRepoExists(repository.getLink()))) {
        log.info("{}: Repository doesn't exist, ignoring the event : {}", GIT_BRANCH_HOOK_EVENT, webhookDTO);
        return;
      }

      switch (action) {
        case CREATE:
          String newBranch = getBranchName(scmParsedWebhookResponse);
          // Create new record with UNSYNCED status as its a new branch, if not already exists
          if (gitBranchService.get(webhookDTO.getAccountId(), repository.getLink(), newBranch) == null) {
            gitBranchService.save(prepareGitBranch(webhookDTO));
          } else {
            log.info("{} : Branch already exists, ignoring the event : {}", GIT_BRANCH_HOOK_EVENT, webhookDTO);
          }
          break;
        case DELETE:
          final String deletedBranch = getBranchName(scmParsedWebhookResponse);
          final DeleteResult deleteResult =
              gitBranchService.delete(repository.getLink(), deletedBranch, webhookDTO.getAccountId());
          log.info("{}: Delete branch result : [{}] for branch name: [{}], repoUrl: [{}], account: [{}] ",
              GIT_BRANCH_HOOK_EVENT, deleteResult, deletedBranch, repository.getLink(), webhookDTO.getAccountId());
          break;
        default:
          log.error("New action type encountered in branch hook event {}", action);
      }

    } catch (Exception exception) {
      log.error("{} : Exception while processing webhook event : {}", GIT_BRANCH_HOOK_EVENT, webhookDTO, exception);
    }
  }

  // ------------------------- PRIVATE METHODS --------------------------

  private GitBranch prepareGitBranch(WebhookDTO webhookDTO) {
    Repository repository = webhookDTO.getParsedResponse().getBranch().getRepo();

    return GitBranch.builder()
        .accountIdentifier(webhookDTO.getAccountId())
        .branchName(getBranchName(webhookDTO.getParsedResponse()))
        .branchSyncStatus(BranchSyncStatus.UNSYNCED)
        .repoURL(repository.getLink())
        .build();
  }

  private String getBranchName(ParseWebhookResponse parseWebhookResponse) {
    return parseWebhookResponse.getBranch().getRef().getName();
  }
}
