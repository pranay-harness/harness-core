package io.harness.ngtriggers.helpers;

import com.google.inject.Inject;

import io.harness.cdng.pipeline.helpers.NGPipelineExecuteHelper;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGTriggerWebhookExecutionHelper {
  private final NGTriggerService ngTriggerService;
  private final NGPipelineExecuteHelper ngPipelineExecuteHelper;
  // a trigger utils field inside this class for the scm parsing

  // return type can change
  public boolean parsePayloadAndExecute(String accountIdentifier, String eventPayload) {
    // authenticate using the account identifier

    // parse the eventPayload using the trigger utils
    // dummy parseWebhookResponse added for now
    ParseWebhookResponse parseWebhookResponse = ParseWebhookResponse.newBuilder().build();

    // from the parsed payload, retrieve the repo url
    String repoUrl = getRepoUrl(parseWebhookResponse);

    // use the repo url to query the db and get all triggers corresponding to the url
    List<NGTriggerEntity> listOfTriggers = getTriggers(accountIdentifier, repoUrl);

    // filter the list of triggers based on event, action, condition evaluations
    Optional<NGTriggerEntity> triggerEntity = getTriggerToExecute(listOfTriggers, parseWebhookResponse);

    // if any trigger remains, execute the target of that trigger. return true in this case, false if no trigger matches
    // the condition
    // execute pipeline
    return triggerEntity.filter(this ::executePipeline).isPresent();
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

  private Optional<NGTriggerEntity> getTriggerToExecute(
      List<NGTriggerEntity> listOfTriggers, ParseWebhookResponse parseWebhookResponse) {
    // filter based on events, actions, etc

    // dummy return value
    return Optional.empty();
  }

  private boolean executePipeline(NGTriggerEntity triggerEntity) {
    // execute pipeline by calling the relevant method in ngPipelineExecuteHelper

    // true if it was executed, false otherwise
    return true;
  }
}
