package io.harness.ng.core.gitsync;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.EntityType;
import io.harness.exception.UnsupportedOperationException;
import io.harness.git.model.ChangeType;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GitChangeSetHandler {
  @Inject GitChangeProcessorService gitChangeProcessorService;
  @Inject Set<YamlHandler> yamlHandlerSet;

  // todo(abhinav): change void to gitProcessing result after merge
  void process(GitSyncChanges gitSyncChanges) {
    if (gitSyncChanges == null || isEmpty(gitSyncChanges.getGitSyncEntitiesList())) {
      return;
    }
    gitChangeProcessorService.sort(gitSyncChanges.getGitSyncEntitiesList());
    final List<String> collect =
        gitSyncChanges.getGitSyncEntitiesList()
            .stream()
            .map(gitSyncEntities
                -> gitSyncEntities.getGitSyncChangeSets()
                       .stream()
                       .map(gitSyncChangeSet -> handleGitChangeSet(gitSyncEntities.getEntityType(), gitSyncChangeSet))
                       .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
  }

  private String handleGitChangeSet(EntityType entityType, GitSyncChangeSet gitSyncChangeSet) {
    if (gitSyncChangeSet.getGitFileChange() == null) {
      return null;
    }
    YamlHandler yamlHandlerForChangeSet =
        yamlHandlerSet.stream()
            .filter(yamlHandler -> yamlHandler.canProcess(gitSyncChangeSet.getGitFileChange(), entityType))
            .findFirst()
            .orElseThrow(() -> new UnsupportedOperationException("yaml handler could not be found"));
    final ChangeType changeType = gitSyncChangeSet.getGitFileChange().getChangeType();
    return handleGitChangeSetForChangeType(gitSyncChangeSet, yamlHandlerForChangeSet, changeType);
  }

  private String handleGitChangeSetForChangeType(
      GitSyncChangeSet gitSyncChangeSet, YamlHandler yamlHandlerForChangeSet, ChangeType changeType) {
    switch (changeType) {
      case ADD:
        return yamlHandlerForChangeSet.handleAddition(gitSyncChangeSet.getGitFileChange(),
            gitSyncChangeSet.getProjectIdentifier(), gitSyncChangeSet.getOrgIdentifier(),
            gitSyncChangeSet.getProjectIdentifier());
      case MODIFY:
        return yamlHandlerForChangeSet.handleModify(gitSyncChangeSet.getGitFileChange(),
            gitSyncChangeSet.getProjectIdentifier(), gitSyncChangeSet.getOrgIdentifier(),
            gitSyncChangeSet.getProjectIdentifier());
      case DELETE:
        return yamlHandlerForChangeSet.handleDeletion(gitSyncChangeSet.getGitFileChange(),
            gitSyncChangeSet.getProjectIdentifier(), gitSyncChangeSet.getOrgIdentifier(),
            gitSyncChangeSet.getProjectIdentifier());
      default:
        return null;
    }
  }
}
