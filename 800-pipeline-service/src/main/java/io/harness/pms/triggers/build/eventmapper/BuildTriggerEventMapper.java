package io.harness.pms.triggers.build.eventmapper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ngtriggers.Constants.ARTIFACT_VERSION;
import static io.harness.ngtriggers.Constants.MANIFEST_VERSION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.response.TriggerEventResponse;
import io.harness.ngtriggers.helpers.TriggerEventResponseHelper;
import io.harness.ngtriggers.helpers.TriggerHelper;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.validations.TriggerValidationHandler;
import io.harness.ngtriggers.validations.ValidationResult;
import io.harness.pms.triggers.webhook.helpers.TriggerEventExecutionHelper;
import io.harness.polling.contracts.PollingResponse;
import io.harness.repositories.spring.TriggerEventHistoryRepository;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class BuildTriggerEventMapper {
  private final NGTriggerService ngTriggerService;
  private final TriggerValidationHandler triggerValidationHandler;
  private final TriggerEventExecutionHelper triggerEventExecutionHelper;
  private final NGTriggerElementMapper ngTriggerElementMapper;
  private final TriggerEventHistoryRepository triggerEventHistoryRepository;

  public void consumeBuildTriggerEvent(PollingResponse pollingResponse) {
    String accountId = pollingResponse.getAccountId();

    int signaturesCount = pollingResponse.getSignaturesCount();
    if (signaturesCount <= 0) {
      return;
    }
    List<String> signatures = new ArrayList<>();
    for (int i = 0; i < signaturesCount; i++) {
      signatures.add(pollingResponse.getSignatures(i));
    }

    List<NGTriggerEntity> mappedTriggers =
        ngTriggerService.findBuildTriggersByAccountIdAndSignature(accountId, signatures);
    if (isEmpty(mappedTriggers)) {
      return;
    }

    processTriggerForActivation(mappedTriggers, pollingResponse);
  }

  private void processTriggerForActivation(List<NGTriggerEntity> mappedTriggers, PollingResponse pollingResponse) {
    int versionsCount = pollingResponse.getBuildInfo().getVersionsCount();
    if (versionsCount == 0) {
      log.warn("PollingEvent Came with No Version Info. BuildInfo Name: " + pollingResponse.getBuildInfo().getName());
      return;
    }

    List<NGTriggerEntity> finalTriggers = filterByApplyingValidations(mappedTriggers);

    List<TriggerEventResponse> responses = new ArrayList<>();
    for (NGTriggerEntity ngTriggerEntity : finalTriggers) {
      try {
        replaceBuildFromEvent(ngTriggerEntity, pollingResponse.getBuildInfo().getVersions(0));
        TriggerDetails triggerDetails = ngTriggerElementMapper.toTriggerDetails(ngTriggerEntity.getAccountId(),
            ngTriggerEntity.getOrgIdentifier(), ngTriggerEntity.getProjectIdentifier(), ngTriggerEntity.getYaml());
        responses.add(triggerEventExecutionHelper.triggerEventPipelineExecution(triggerDetails));
      } catch (Exception e) {
        log.error("Error while requesting pipeline execution for Build Trigger: "
            + TriggerHelper.getTriggerRef(ngTriggerEntity));
      }
    }

    if (isNotEmpty(responses)) {
      responses.forEach(response -> triggerEventHistoryRepository.save(TriggerEventResponseHelper.toEntity(response)));
    }
  }

  private void replaceBuildFromEvent(NGTriggerEntity trigger, String version) {
    trigger.setYaml(trigger.getYaml().replaceAll(MANIFEST_VERSION, version).replaceAll(ARTIFACT_VERSION, version));
  }

  @VisibleForTesting
  List<NGTriggerEntity> filterByApplyingValidations(List<NGTriggerEntity> mappedTriggers) {
    List<NGTriggerEntity> finalList = new ArrayList<>();
    NGTriggerEntity currentTrigerBeingProcessed = null;
    for (NGTriggerEntity ngTriggerEntity : mappedTriggers) {
      try {
        currentTrigerBeingProcessed = ngTriggerEntity;
        ValidationResult validationResult = triggerValidationHandler.applyValidations(ngTriggerEntity);
        ngTriggerService.updateTriggerWithValidationStatus(ngTriggerEntity, validationResult);

        if (!validationResult.isSuccess()) {
          log.error("Error while requesting pipeline execution for Build Trigger: "
              + TriggerHelper.getTriggerRef(ngTriggerEntity));
        }

        finalList.add(ngTriggerEntity);
      } catch (Exception e) {
        log.error(String.format("Failed while validating trigger: %s during Build Event Processing",
                      TriggerHelper.getTriggerRef(currentTrigerBeingProcessed)),
            e);
      }
    }

    return finalList;
  }
}
