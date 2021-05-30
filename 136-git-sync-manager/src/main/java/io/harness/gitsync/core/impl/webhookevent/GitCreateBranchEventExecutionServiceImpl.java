package io.harness.gitsync.core.impl.webhookevent;

import static io.harness.gitsync.common.WebhookEventConstants.GIT_CREATE_BRANCH_EVENT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.service.webhookevent.GitCreateBranchEventExecutionService;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.Repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.DX)
@Singleton
@Slf4j
public class GitCreateBranchEventExecutionServiceImpl implements GitCreateBranchEventExecutionService {
  @Inject YamlGitConfigService yamlGitConfigService;

  @Override
  public void processEvent(WebhookDTO webhookDTO) {
    try {
      ParseWebhookResponse scmParsedWebhookResponse = webhookDTO.getParsedResponse();
      if (scmParsedWebhookResponse == null || scmParsedWebhookResponse.getCreateBranch() == null) {
        log.error("{} : Error while consuming webhook Parsed response : {}", GIT_CREATE_BRANCH_EVENT, webhookDTO);
        return;
      }

      Repository repository = scmParsedWebhookResponse.getCreateBranch().getRepo();

      // If repo doesn't exist, ignore the event
      if (Boolean.FALSE.equals(yamlGitConfigService.isRepoExists(repository.getLink()))) {
        log.info("{}: Repository doesn't exist, ignoring the event : {}", GIT_CREATE_BRANCH_EVENT, webhookDTO);
        return;
      }
    } catch (Exception exception) {
      log.error("{} : Exception while processing webhook event : {}", GIT_CREATE_BRANCH_EVENT, webhookDTO, exception);
    }
  }
}
