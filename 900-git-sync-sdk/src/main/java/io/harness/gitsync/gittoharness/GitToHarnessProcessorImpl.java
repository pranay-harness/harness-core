package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.HarnessStringUtils.emptyIfNull;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.ChangeSets;
import io.harness.gitsync.FileProcessingResponse;
import io.harness.gitsync.FileProcessingStatus;
import io.harness.gitsync.GitToHarnessInfo;
import io.harness.gitsync.GitToHarnessProcessRequest;
import io.harness.gitsync.ProcessingFailureStage;
import io.harness.gitsync.ProcessingResponse;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.gitsync.interceptor.GitSyncThreadDecorator;
import io.harness.manage.GlobalContextManager;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class GitToHarnessProcessorImpl implements GitToHarnessProcessor {
  ChangeSetInterceptorService changeSetInterceptorService;
  GitSdkInterface changeSetHelperService;
  Supplier<List<EntityType>> sortOrder;
  GitSyncThreadDecorator gitSyncThreadDecorator;

  @Inject
  public GitToHarnessProcessorImpl(ChangeSetInterceptorService changeSetInterceptorService,
      GitSdkInterface changeSetHelperService, @Named("GitSyncSortOrder") Supplier<List<EntityType>> sortOrder,
      GitSyncThreadDecorator gitSyncThreadDecorator) {
    this.changeSetInterceptorService = changeSetInterceptorService;
    this.changeSetHelperService = changeSetHelperService;
    this.sortOrder = sortOrder;
    this.gitSyncThreadDecorator = gitSyncThreadDecorator;
  }

  /**
   * Processing is a 4 step process.
   * <li><b>Preprocess step.</b> Generally a no op stage.</li>
   * <li><b>Sort step.</b> Changesets are sorted as per sort order.</li>
   * <li><b>Process step.</b> Change sets are processed by calling various service layers.</li>
   * <li><b>Post process step.</b> Collection of all the return data happens.</li>
   */
  @Override
  public ProcessingResponse gitToHarnessProcessingRequest(GitToHarnessProcessRequest gitToHarnessRequest) {
    ChangeSets changeSets = gitToHarnessRequest.getChangeSets();
    Map<String, FileProcessingResponse> processingResponseMap = initializeProcessingResponse(changeSets);
    String accountId = changeSets.getAccountId();

    if (isEmpty(changeSets.getChangeSetList())) {
      return ProcessingResponse.newBuilder().setAccountId(accountId).build();
    }
    if (preProcessStage(changeSets, processingResponseMap)) {
      return flattenProcessingResponse(processingResponseMap, accountId, ProcessingFailureStage.RECEIVE_STAGE);
    }

    if (sortStage(changeSets, processingResponseMap)) {
      return flattenProcessingResponse(processingResponseMap, accountId, ProcessingFailureStage.SORT_STAGE);
    }
    if (processStage(changeSets, processingResponseMap, gitToHarnessRequest)) {
      return flattenProcessingResponse(processingResponseMap, accountId, ProcessingFailureStage.PROCESS_STAGE);
    }

    if (postProcessStage(changeSets, processingResponseMap, accountId)) {
      return flattenProcessingResponse(processingResponseMap, accountId, ProcessingFailureStage.PROCESS_STAGE);
    }

    return flattenProcessingResponse(processingResponseMap, accountId, null);
  }

  private boolean postProcessStage(
      ChangeSets changeSets, Map<String, FileProcessingResponse> processingResponseMap, String accountId) {
    try {
      final ProcessingResponse processingResponse = flattenProcessingResponse(processingResponseMap, accountId, null);
      changeSetInterceptorService.postChangeSetProcessing(processingResponse, changeSets.getAccountId());
    } catch (Exception e) {
      return true;
    }
    return false;
  }

  private boolean processStage(ChangeSets changeSets, Map<String, FileProcessingResponse> processingResponseMap,
      GitToHarnessProcessRequest gitToHarnessRequest) {
    try {
      // todo(abhinav): Do parallel processing.
      for (ChangeSet changeSet : changeSets.getChangeSetList()) {
        try (GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {
          GlobalContextManager.upsertGlobalContextRecord(
              createGitEntityInfo(gitToHarnessRequest.getGitToHarnessBranchInfo(), changeSet));
          changeSetHelperService.process(changeSet);
          updateFileProcessingResponse(FileProcessingStatus.SUCCESS, null, processingResponseMap, changeSet.getId());
          processingResponseMap.put(changeSet.getId(),
              FileProcessingResponse.newBuilder()
                  .setAccountId(changeSet.getAccountId())
                  .setId(changeSet.getId())
                  .setStatus(FileProcessingStatus.SUCCESS)
                  .build());
        } catch (Exception e) {
          log.error("Exception {}", changeSet.getFilePath(), e);
          updateFileProcessingResponse(
              FileProcessingStatus.FAILURE, e.getMessage(), processingResponseMap, changeSet.getId());
        }
      }
    } catch (Exception e) {
      return true;
    }
    return false;
  }

  private GitSyncBranchContext createGitEntityInfo(GitToHarnessInfo gitToHarnessBranchInfo, ChangeSet changeSet) {
    String[] pathSplited = emptyIfNull(changeSet.getFilePath()).split(GitSyncConstants.FOLDER_PATH);
    if (pathSplited.length != 2) {
      throw new InvalidRequestException(
          String.format("The path %s doesn't contain the .harness folder, thus this file won't be processed",
              changeSet.getFilePath()));
    }
    String folderPath = pathSplited[0] + GitSyncConstants.FOLDER_PATH;
    String filePath = pathSplited[1];
    return GitSyncBranchContext.builder()
        .gitBranchInfo(GitEntityInfo.builder()
                           .branch(gitToHarnessBranchInfo.getBranch())
                           .folderPath(folderPath)
                           .filePath(filePath)
                           .yamlGitConfigId(gitToHarnessBranchInfo.getYamlGitConfigId())
                           .lastObjectId(changeSet.getObjectId() == null ? null : changeSet.getObjectId().getValue())
                           .isSyncFromGit(true)
                           .build())
        .build();
  }

  private boolean sortStage(ChangeSets changeSets, Map<String, FileProcessingResponse> processingResponseMap) {
    try {
      final List<ChangeSet> sortedChangeSets = sortChangeSets(sortOrder, changeSets.getChangeSetList());
      changeSetInterceptorService.postChangeSetSort(sortedChangeSets, changeSets.getAccountId());
    } catch (Exception e) {
      updateFileProcessingResponseForAllChangeSets(processingResponseMap, FileProcessingStatus.SKIPPED, e.getMessage());
      return true;
    }
    return false;
  }

  private boolean preProcessStage(ChangeSets changeSets, Map<String, FileProcessingResponse> processingResponseMap) {
    try {
      changeSetInterceptorService.onChangeSetReceive(changeSets, changeSets.getAccountId());
    } catch (Exception e) {
      updateFileProcessingResponseForAllChangeSets(processingResponseMap, FileProcessingStatus.SKIPPED, e.getMessage());
      return true;
    }
    return false;
  }

  private ProcessingResponse flattenProcessingResponse(Map<String, FileProcessingResponse> processingResponseMap,
      String accountId, ProcessingFailureStage processingFailureStage) {
    final List<FileProcessingResponse> fileProcessingResponses = new ArrayList<>(processingResponseMap.values());
    final ProcessingResponse.Builder processingResponseBuilder =
        ProcessingResponse.newBuilder().addAllResponse(fileProcessingResponses).setAccountId(accountId);
    if (processingFailureStage != null) {
      processingResponseBuilder.setProcessingFailureStage(processingFailureStage);
    }
    return processingResponseBuilder.build();
  }

  private void updateFileProcessingResponseForAllChangeSets(
      Map<String, FileProcessingResponse> processingResponseMap, FileProcessingStatus status, String message) {
    processingResponseMap.forEach(
        (key, value) -> updateFileProcessingResponse(status, message, processingResponseMap, key));
  }

  private void updateFileProcessingResponse(
      FileProcessingStatus status, String message, Map<String, FileProcessingResponse> response, String key) {
    final FileProcessingResponse responseValue = response.get(key);
    final FileProcessingResponse.Builder fileProcessingResponseBuilder = FileProcessingResponse.newBuilder()
                                                                             .setStatus(status)
                                                                             .setAccountId(responseValue.getAccountId())
                                                                             .setId(responseValue.getId());
    if (!isEmpty(message)) {
      fileProcessingResponseBuilder.setErrorMsg(message);
    }
    response.put(key, fileProcessingResponseBuilder.build());
  }

  private Map<String, FileProcessingResponse> initializeProcessingResponse(ChangeSets changeSets) {
    Map<String, FileProcessingResponse> processingResponseMap = new HashMap<>();
    changeSets.getChangeSetList().forEach(changeSet
        -> processingResponseMap.put(changeSet.getId(),
            FileProcessingResponse.newBuilder()
                .setId(changeSet.getId())
                .setAccountId(changeSet.getAccountId())
                .setStatus(FileProcessingStatus.UNPROCESSED)
                .build()));
    return processingResponseMap;
  }

  private List<ChangeSet> sortChangeSets(Supplier<List<EntityType>> sortOrder, List<ChangeSet> changeSetList) {
    return changeSetList.stream().sorted(new ChangeSetSortComparator(sortOrder.get())).collect(Collectors.toList());
  }
}