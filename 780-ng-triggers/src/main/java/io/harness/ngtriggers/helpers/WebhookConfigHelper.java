package io.harness.ngtriggers.helpers;

import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;

import java.util.*;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WebhookConfigHelper {
  public Map<WebhookSourceRepo, List<WebhookEvent>> getSourceRepoToEvent() {
    Map<WebhookSourceRepo, List<WebhookEvent>> map = new HashMap<>();
    map.put(WebhookSourceRepo.GITHUB, new ArrayList<>(WebhookEvent.githubEvents));
    map.put(WebhookSourceRepo.GITLAB, new ArrayList<>(WebhookEvent.gitlabEvents));
    map.put(WebhookSourceRepo.BITBUCKET, new ArrayList<>(WebhookEvent.bitbucketEvents));
    return map;
  }

  public List<WebhookAction> getActionsList(WebhookSourceRepo sourceRepo, WebhookEvent event) {
    if (sourceRepo == WebhookSourceRepo.GITHUB) {
      return new ArrayList<>(WebhookAction.getGithubActionForEvent(event));
    } else if (sourceRepo == WebhookSourceRepo.BITBUCKET) {
      return new ArrayList<>(WebhookAction.getBitbucketActionForEvent(event));
    } else if (sourceRepo == WebhookSourceRepo.GITLAB) {
      return new ArrayList<>(WebhookAction.getGitLabActionForEvent(event));
    } else {
      return Collections.emptyList();
    }
  }
}
