package io.harness.ng.ngtriggers.helpers;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.helpers.NGPipelineExecuteHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.NGTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.beans.target.TargetSpec;
import io.harness.ngtriggers.beans.target.pipeline.PipelineTargetSpec;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.WebhookEventPayloadParser;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGTriggerWebhookExecutionHelper {
  private final NGTriggerService ngTriggerService;
  private final NGPipelineExecuteHelper ngPipelineExecuteHelper;
  private final WebhookEventPayloadParser webhookEventPayloadParser;
  // a trigger utils field inside this class for the scm parsing
  // private TriggerUtilsNG triggerUtils;

  // return type can change
  public boolean handleTriggerWebhookEvent(TriggerWebhookEvent triggerWebhookEvent) {
    // parse the eventPayload using the trigger utils
    WebhookPayloadData webhookPayloadData = parseEventData(triggerWebhookEvent);

    // from the parsed payload, retrieve the repo url
    // use the repo url to query the db and get all triggers corresponding to the url
    List<NGTriggerEntity> triggersForRepo = retrieveTriggersConfiguredForRepo(triggerWebhookEvent, webhookPayloadData);
    if (EmptyPredicate.isEmpty(triggersForRepo)) {
      return true;
    }

    applyFiltersAndInvokePipelineExecutionIfNeeded(webhookPayloadData, triggersForRepo);

    // if any trigger remains, execute the target of that trigger. return true in this case, false if no trigger matches
    // the condition
    // execute pipeline
    // return triggerEntity.filter(this ::executePipeline).isPresent();
    return false;
  }

  @VisibleForTesting
  void applyFiltersAndInvokePipelineExecutionIfNeeded(
      WebhookPayloadData webhookPayloadData, List<NGTriggerEntity> triggersForRepo) {
    for (NGTriggerEntity ngTriggerEntity : triggersForRepo) {
      if (checkTriggerEligibility(webhookPayloadData, ngTriggerEntity)) {
        // test
      }
    }
  }

  private boolean checkTriggerEligibility(WebhookPayloadData webhookPayloadData, NGTriggerEntity ngTriggerEntity) {
    try {
      NGTriggerConfig ngTriggerConfig = NGTriggerElementMapper.toTriggerConfig(ngTriggerEntity.getYaml());
      NGTriggerSpec spec = ngTriggerConfig.getSource().getSpec();
      if (!WebhookTriggerConfig.class.isAssignableFrom(spec.getClass())) {
        return false;
        // Some log
      }

      WebhookTriggerConfig webhookTriggerConfig = (WebhookTriggerConfig) spec;
      WebhookTriggerSpec webhookTriggerConfigSpec = webhookTriggerConfig.getSpec();
      boolean conditionMatched = WebhookEventFilter.checkIfEventTypeMatches(
          webhookPayloadData.getWebhookEvent().getType(), webhookTriggerConfigSpec.getEvent());

      conditionMatched =
          conditionMatched && WebhookEventFilter.checkIfActionMatches(webhookPayloadData, webhookTriggerConfigSpec);
      return conditionMatched;
    } catch (Exception e) {
      log.error(new StringBuilder(128)
                    .append("Failed while evaluating Trigger: ")
                    .append(ngTriggerEntity.getIdentifier())
                    .append(", For Account: ")
                    .append(ngTriggerEntity.getAccountId())
                    .append(", correlationId for event is: ")
                    .append(webhookPayloadData.getOriginalEvent().getUuid())
                    .toString());
    }

    return false;
  }

  private void resolveRuntimeInputAndSubmitExecutionRequest(NGTriggerEntity trigger, NGTriggerConfig ngTriggerConfig) {
    TargetSpec targetSpec = ngTriggerConfig.getTarget().getSpec();
    if (PipelineTargetSpec.class.isAssignableFrom(targetSpec.getClass())) {
      PipelineTargetSpec pipelineTargetSpec = (PipelineTargetSpec) targetSpec;
      ngPipelineExecuteHelper.runPipelineWithInputSetPipelineYaml(trigger.getAccountId(), trigger.getOrgIdentifier(),
          trigger.getProjectIdentifier(), trigger.getTargetIdentifier(), pipelineTargetSpec.getRuntimeInputYaml(),
          false, EmbeddedUser.builder().name("trigger-autouser").uuid("trigger-autouser").build());
    }
  }

  private List<NGTriggerEntity> retrieveTriggersConfiguredForRepo(
      TriggerWebhookEvent triggerWebhookEvent, WebhookPayloadData webhookPayloadData) {
    String repoUrl = webhookPayloadData.getRepository().getLink();
    Page<NGTriggerEntity> triggerPage = (Page<NGTriggerEntity>) ngTriggerService.listWebhookTriggers(
        triggerWebhookEvent.getAccountId(), repoUrl, false);

    List<NGTriggerEntity> listOfTriggers = triggerPage.get().collect(Collectors.toList());
    return listOfTriggers == null ? Collections.EMPTY_LIST : listOfTriggers;
  }

  // Add error handling
  private WebhookPayloadData parseEventData(TriggerWebhookEvent triggerWebhookEvent) {
    return webhookEventPayloadParser.parseEvent(triggerWebhookEvent);
  }

  private String getRepoUrl(ParseWebhookResponse parseWebhookResponse) {
    // dummy return value
    return parseWebhookResponse.toString();
  }

  private List<NGTriggerEntity> getTriggers(String accountIdentifier, String repoUrl) {
    // create criteria, send it to ngTriggerService.list();

    // dummy return value
    return Collections.emptyList();
  }

  private List<NGTriggerEntity> applyFiltering(
      List<NGTriggerEntity> listOfTriggers, WebhookPayloadData webhookPayloadData) {
    // filter based on events, actions, etc

    // dummy return value
    return Collections.EMPTY_LIST;
  }

  private boolean executePipeline(NGTriggerEntity triggerEntity) {
    // execute pipeline by calling the relevant method in ngPipelineExecuteHelper

    // true if it was executed, false otherwise
    return true;
  }
}
