/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.product.ci.scm.proto.ContentType;
import io.harness.product.ci.scm.proto.FileChange;
import io.harness.product.ci.scm.proto.FindFilesInBranchRequest;
import io.harness.product.ci.scm.proto.FindFilesInBranchResponse;
import io.harness.product.ci.scm.proto.PageRequest;
import io.harness.product.ci.scm.proto.Provider;
import io.harness.product.ci.scm.proto.SCMGrpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * This is a fork and join task which we are using to get all files belonging in
 * the folders.
 */
@Slf4j
@OwnedBy(DX)
public class GetFilesInFolderForkTask extends RecursiveTask<List<FileChange>> {
  SCMGrpc.SCMBlockingStub scmBlockingStub;
  private String folderPath;
  private Provider provider;
  private String ref;
  private String slug;

  @Builder
  public GetFilesInFolderForkTask(
      String folderPath, Provider provider, String ref, String slug, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    this.folderPath = folderPath;
    this.provider = provider;
    this.ref = ref;
    this.slug = slug;
    this.scmBlockingStub = scmBlockingStub;
  }

  /**
   * This is the main function which will be called by all the tasks, every task needs to
   * get files belonging to its folder.
   *
   * This function provides all the files belonging to this folder and then it creates
   * subtasks for every sub folder
   */
  @Override
  protected List<FileChange> compute() {
    List<FileChange> filesList = new ArrayList<>();
    String updatedFolderPath = folderPath.endsWith("/") ? folderPath.substring(0, folderPath.length() - 1) : folderPath;
    FindFilesInBranchRequest.Builder findFilesInBranchRequest =
        FindFilesInBranchRequest.newBuilder()
            .setRef(ref)
            .setSlug(slug)
            .setProvider(provider)
            .setPath(updatedFolderPath)
            .setPagination(PageRequest.newBuilder().setPage(1).build());
    List<FileChange> filesInBranch = getAllFilesPresentInFolder(findFilesInBranchRequest);
    List<String> newFoldersToBeProcessed = getListOfNewFoldersToBeProcessed(filesInBranch);
    List<GetFilesInFolderForkTask> tasksForSubFolders = createTasksForSubFolders(newFoldersToBeProcessed);
    addFilesOfThisFolder(filesList, filesInBranch);
    addFilesOfSubfolders(filesList, tasksForSubFolders);
    return filesList;
  }

  List<FileChange> getAllFilesPresentInFolder(FindFilesInBranchRequest.Builder findFilesInBranchRequest) {
    FindFilesInBranchResponse filesInBranchResponse = null;
    List<FileChange> allFilesInThisFolder = new ArrayList<>();
    do {
      try {
        filesInBranchResponse = scmBlockingStub.findFilesInBranch(findFilesInBranchRequest.build());
        allFilesInThisFolder.addAll(filesInBranchResponse.getFileList());
        findFilesInBranchRequest.setPagination(
            PageRequest.newBuilder().setPage(filesInBranchResponse.getPagination().getNext()).build());
      } catch (Exception ex) {
        log.error(
            "Error while getting files from git for the ref {} in slug {} for folder {}", ref, slug, folderPath, ex);
      }
    } while (hasMoreFiles(filesInBranchResponse));
    return allFilesInThisFolder;
  }

  private boolean hasMoreFiles(FindFilesInBranchResponse filesInBranchResponse) {
    return filesInBranchResponse != null && filesInBranchResponse.getPagination() != null
        && filesInBranchResponse.getPagination().getNext() != 0;
  }

  private List<GetFilesInFolderForkTask> createTasksForSubFolders(List<String> newFoldersToBeProcessed) {
    List<GetFilesInFolderForkTask> tasks = new ArrayList<>();
    for (String folder : newFoldersToBeProcessed) {
      GetFilesInFolderForkTask task = GetFilesInFolderForkTask.builder()
                                          .ref(ref)
                                          .folderPath(folder)
                                          .provider(provider)
                                          .scmBlockingStub(scmBlockingStub)
                                          .slug(slug)
                                          .build();
      task.fork();
      tasks.add(task);
    }
    return tasks;
  }

  private void addFilesOfSubfolders(List<FileChange> filesList, List<GetFilesInFolderForkTask> tasksForSubFolders) {
    for (GetFilesInFolderForkTask task : tasksForSubFolders) {
      filesList.addAll(task.join());
    }
  }

  private void addFilesOfThisFolder(List<FileChange> filesList, List<FileChange> filesInBranchResponse) {
    List<FileChange> fileChangesInThisFolder = emptyIfNull(filesInBranchResponse)
                                                   .stream()
                                                   .filter(change -> change.getContentType() == ContentType.FILE)
                                                   .collect(toList());
    filesList.addAll(fileChangesInThisFolder);
  }

  private List<String> getListOfNewFoldersToBeProcessed(List<FileChange> filesInBranch) {
    return emptyIfNull(filesInBranch)
        .stream()
        .filter(fileChange -> fileChange.getContentType() == ContentType.DIRECTORY)
        .map(FileChange::getPath)
        .collect(toList());
  }

  public List<FileChange> createForkJoinTask(Set<String> foldersList) {
    List<GetFilesInFolderForkTask> tasks = new ArrayList<>();
    ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
    for (String folder : foldersList) {
      GetFilesInFolderForkTask task = GetFilesInFolderForkTask.builder()
                                          .ref(ref)
                                          .folderPath(folder)
                                          .provider(provider)
                                          .scmBlockingStub(scmBlockingStub)
                                          .slug(slug)
                                          .build();
      forkJoinPool.execute(task);
      tasks.add(task);
    }
    List<FileChange> allFiles = new ArrayList<>();
    for (GetFilesInFolderForkTask task : tasks) {
      allFiles.addAll(emptyIfNull(task.join()));
    }
    return allFiles;
  }
}
