package io.harness.delegate.event.handler;

import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.beans.FeatureName.TRIGGER_PROFILE_SCRIPT_EXECUTION_WF;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.compare;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.WorkflowType;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileKeys;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.ff.FeatureFlagService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateProfileObserver;

import software.wings.beans.ExecutionArgs;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.WorkflowExecutionService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
@Singleton
public class DelegateProfileEventHandler implements DelegateProfileObserver {
  public static final String MUST_EXECUTE_ON_DELEGATE_VAR_NAME = "mustExecuteOnDelegateId";
  public static final String PROFILE_SCRIPT_CONTENT_VAR_NAME = "scriptContent";

  // Name of the system defined workflow
  public static final String PROFILE_SCRIPT_EXECUTION_WORKFLOW_NAME = "PROFILE_SCRIPT_EXECUTION";

  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private HPersistence persistence;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private DelegateService delegateService;

  @Override
  public void onProfileUpdated(@NonNull DelegateProfile originalProfile, @NonNull DelegateProfile updatedProfile) {
    // Trigger profile script workflow execution, foreach of the delegates, if script is changed
    if (featureFlagService.isEnabled(TRIGGER_PROFILE_SCRIPT_EXECUTION_WF, originalProfile.getAccountId())) {
      if (isNotBlank(updatedProfile.getStartupScript())
          && compare(originalProfile.getStartupScript(), updatedProfile.getStartupScript()) != 0) {
        Set<String> delegateIds = fetchProfileDelegateIds(updatedProfile.getAccountId(), updatedProfile.getUuid());

        for (String delegateId : delegateIds) {
          triggerProfileScriptWorkflowExecution(
              updatedProfile.getAccountId(), delegateId, updatedProfile.getStartupScript());
        }
      }
    }

    if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, updatedProfile.getAccountId())) {
      boolean selectorsChanged = (hasNone(originalProfile.getSelectors()) && hasSome(updatedProfile.getSelectors()))
          || (hasSome(originalProfile.getSelectors()) && hasNone(updatedProfile.getSelectors()))
          || (hasSome(originalProfile.getSelectors()) && hasSome(updatedProfile.getSelectors())
              && !CollectionUtils.isEqualCollection(originalProfile.getSelectors(), updatedProfile.getSelectors()));

      boolean scopingRulesChanged =
          (hasNone(originalProfile.getScopingRules()) && hasSome(updatedProfile.getScopingRules()))
          || (hasSome(originalProfile.getScopingRules()) && hasNone(updatedProfile.getScopingRules()))
          || (hasSome(originalProfile.getScopingRules()) && hasSome(updatedProfile.getScopingRules())
              && !CollectionUtils.isEqualCollection(
                  originalProfile.getScopingRules(), updatedProfile.getScopingRules()));

      if (selectorsChanged || scopingRulesChanged) {
        Set<String> delegateIds = fetchProfileDelegateIds(updatedProfile.getAccountId(), updatedProfile.getUuid());

        for (String delegateId : delegateIds) {
          delegateService.regenerateCapabilityPermissions(updatedProfile.getAccountId(), delegateId);
        }
      }
    }
  }

  @Override
  public void onProfileApplied(@NonNull String accountId, @NonNull String delegateId, @NonNull String profileId) {
    if (featureFlagService.isEnabled(TRIGGER_PROFILE_SCRIPT_EXECUTION_WF, accountId)) {
      // Trigger profile script workflow execution, for the given delegate
      DelegateProfile appliedProfile = persistence.createQuery(DelegateProfile.class)
                                           .filter(DelegateProfileKeys.accountId, accountId)
                                           .filter(DelegateProfileKeys.uuid, profileId)
                                           .get();

      if (appliedProfile != null && isNotBlank(appliedProfile.getStartupScript())) {
        triggerProfileScriptWorkflowExecution(accountId, delegateId, appliedProfile.getStartupScript());
      }
    }

    if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, accountId)) {
      delegateService.regenerateCapabilityPermissions(accountId, delegateId);
    }
  }

  @Override
  public void onProfileSelectorsUpdated(String accountId, String profileId) {
    processProfileScopeAndSelectorChanges(accountId, profileId);
  }

  @Override
  public void onProfileScopesUpdated(String accountId, String profileId) {
    processProfileScopeAndSelectorChanges(accountId, profileId);
  }

  private void processProfileScopeAndSelectorChanges(String accountId, String profileId) {
    if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, accountId)) {
      Set<String> delegateIds = fetchProfileDelegateIds(accountId, profileId);

      for (String delegateId : delegateIds) {
        delegateService.regenerateCapabilityPermissions(accountId, delegateId);
      }
    }
  }

  private Set<String> fetchProfileDelegateIds(String accountId, String profileId) {
    return persistence.createQuery(Delegate.class)
        .filter(DelegateKeys.accountId, accountId)
        .filter(DelegateKeys.delegateProfileId, profileId)
        .asKeyList()
        .stream()
        .map(key -> (String) key.getId())
        .collect(toSet());
  }

  private void triggerProfileScriptWorkflowExecution(String accountId, String delegateId, String profileScriptContent) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore1 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      Workflow workflow = persistence.createQuery(Workflow.class)
                              .filter(WorkflowKeys.accountId, accountId)
                              .filter(WorkflowKeys.name, PROFILE_SCRIPT_EXECUTION_WORKFLOW_NAME)
                              .get();

      if (workflow != null) {
        log.info("Triggering profile script execution workflow for the delegate...");

        final Map<String, String> wfVars = ImmutableMap.<String, String>builder()
                                               .put(MUST_EXECUTE_ON_DELEGATE_VAR_NAME, delegateId)
                                               .put(PROFILE_SCRIPT_CONTENT_VAR_NAME, profileScriptContent)
                                               .build();

        workflowExecutionService.triggerOrchestrationExecution(workflow.getAppId(), null, workflow.getUuid(),
            ExecutionArgs.builder().workflowType(WorkflowType.ORCHESTRATION).workflowVariables(wfVars).build(), null);
      } else {
        log.warn("No {} system workflow found. Profile script cannot be applied to the given delegate.",
            PROFILE_SCRIPT_EXECUTION_WORKFLOW_NAME);
      }
    }
  }
}
