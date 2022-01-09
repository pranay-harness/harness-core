/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.sync;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.beans.SettingAttribute.SettingCategory.AZURE_ARTIFACTS;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;
import static software.wings.beans.security.UserGroup.ACCOUNT_ID_KEY;
import static software.wings.beans.yaml.YamlConstants.GIT_SETUP_RBAC_PREFIX;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlType.GLOBAL_TEMPLATE_LIBRARY;
import static software.wings.service.impl.yaml.sync.GitSyncErrorUtils.setYamlContent;

import io.harness.beans.PageRequest;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.SettingsService;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitFileActivity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class GitSyncRBACHelper {
  @Inject YamlHelper yamlHelper;
  @Inject SettingsService settingsService;

  @Data
  @Builder
  private static class YamlFileDetails {
    String yamlFilePath;
    String entityName;
    SettingCategory type;
  }
  public List<GitFileActivity> populateUserHasPermissionForFileField(
      List<GitFileActivity> gitFileActivities, String accountId) {
    if (isEmpty(gitFileActivities)) {
      return Collections.emptyList();
    }
    List<GitFileActivity> filteredFileActivities = new ArrayList<>();
    List<GitFileActivity> accountLevelActivities = new ArrayList<>();
    for (GitFileActivity gitFileActivity : gitFileActivities) {
      if (yamlHelper.isAccountLevelEntity(gitFileActivity.getFilePath())) {
        accountLevelActivities.add(gitFileActivity);
      } else {
        filteredFileActivities.add(gitFileActivity);
      }
    }
    filteredFileActivities.addAll(filterAccountLevelEntitiesAccessibleToUser(accountLevelActivities, accountId));
    return filteredFileActivities;
  }

  public List<GitSyncError> populateUserHasPermissionForFileFieldInErrors(
      List<GitSyncError> errorFiles, String accountId) {
    if (isEmpty(errorFiles)) {
      return Collections.emptyList();
    }
    List<GitSyncError> filteredFileActivities = new ArrayList<>();
    List<GitSyncError> accountLevelActivities = new ArrayList<>();
    for (GitSyncError errorFile : errorFiles) {
      if (yamlHelper.isAccountLevelEntity(errorFile.getYamlFilePath())) {
        accountLevelActivities.add(errorFile);
      } else {
        filteredFileActivities.add(errorFile);
      }
    }
    filteredFileActivities.addAll(filterAccountLevelErrorsAccessibleToUser(accountLevelActivities, accountId));
    return filteredFileActivities;
  }

  private List<GitSyncError> filterAccountLevelErrorsAccessibleToUser(List<GitSyncError> errors, String accountId) {
    if (isEmpty(errors)) {
      return Collections.emptyList();
    }
    List<String> yamlFilePaths = errors.stream().map(GitSyncError::getYamlFilePath).collect(Collectors.toList());

    Set<String> accountLevelFileNamesAccessibleToUser = getAccountLevelFilesAccessibleToUser(yamlFilePaths, accountId);
    errors.stream()
        .filter(error -> !accountLevelFileNamesAccessibleToUser.contains(error.getYamlFilePath()))
        .forEach(error -> {
          error.setUserDoesNotHavePermForFile(true);
          setYamlContent(error);
        });
    return errors;
  }

  private List<GitFileActivity> filterAccountLevelEntitiesAccessibleToUser(
      List<GitFileActivity> gitFileActivities, String accountId) {
    if (isEmpty(gitFileActivities)) {
      return Collections.emptyList();
    }
    List<String> yamlFilePaths =
        gitFileActivities.stream().map(GitFileActivity::getFilePath).collect(Collectors.toList());

    Set<String> accountLevelFileNamesAccessibleToUser = getAccountLevelFilesAccessibleToUser(yamlFilePaths, accountId);
    gitFileActivities.stream()
        .filter(activity -> !accountLevelFileNamesAccessibleToUser.contains(activity.getFilePath()))
        .forEach(activity -> {
          activity.setUserDoesNotHavePermForFile(true);
          activity.setFileContent(null);
        });
    return gitFileActivities;
  }

  private Set<String> getAccountLevelFilesAccessibleToUser(List<String> accountLevelFiles, String accountId) {
    if (isEmpty(accountLevelFiles)) {
      return Collections.emptySet();
    }
    List<YamlFileDetails> filesBelongingToSettingAttributeCollection = new ArrayList<>();
    List<String> filesBelongingToTemplateLibrary = new ArrayList<>();

    for (String filePath : accountLevelFiles) {
      YamlType yamlType = yamlHelper.getYamlTypeFromSettingAttributePath(filePath);
      if (yamlType != null) {
        // Its a setting Attribute
        // move it to different function
        filesBelongingToSettingAttributeCollection.add(
            YamlFileDetails.builder()
                .yamlFilePath(filePath)
                .type(yamlHelper.getSettingAttributeCategoryFromYamlType(yamlType))
                .entityName(
                    yamlHelper.extractEntityNameFromYamlPath(yamlType.getPathExpression(), filePath, PATH_DELIMITER))
                .build());
      } else {
        yamlType = yamlHelper.getYamlTypeOfAccountLevelEntity(filePath);
        if (yamlType != null) {
          if (yamlType == GLOBAL_TEMPLATE_LIBRARY) {
            filesBelongingToTemplateLibrary.add(filePath);
          } else {
            log.info(GIT_SETUP_RBAC_PREFIX + " Cannot found account level yamlType for the file [{}]", filePath);
          }
        } else {
          log.info(GIT_SETUP_RBAC_PREFIX + " Cannot found account level yamlType for the file [{}]", filePath);
        }
      }
    }
    Set<String> allFilesAccessibleToUser = new HashSet<>();
    allFilesAccessibleToUser.addAll(
        getSettingAttributeFileAccessibleToUser(filesBelongingToSettingAttributeCollection, accountId));
    // There is no rbac on template library
    allFilesAccessibleToUser.addAll(filesBelongingToTemplateLibrary);
    return allFilesAccessibleToUser;
  }

  private Set<String> getSettingAttributeFileAccessibleToUser(List<YamlFileDetails> fileDetails, String accountId) {
    if (isEmpty(fileDetails)) {
      return Collections.emptySet();
    }
    // Move it to different function
    List<String> fileNames = fileDetails.stream().map(YamlFileDetails::getEntityName).collect(Collectors.toList());
    PageRequest<SettingAttribute> settingAttributePageRequest =
        aPageRequest()
            .addFilter(ACCOUNT_ID_KEY, EQ, accountId)
            .addFilter(SettingAttributeKeys.name, IN, fileNames.toArray())
            .build();
    List<SettingAttribute> settingAttributes = settingsService.list(settingAttributePageRequest, null, null);
    if (isEmpty(settingAttributes)) {
      return Collections.emptySet();
    }
    Map<String, List<SettingCategory>> nameSettingCategoryMap = mapOfNameAndSettingAttributes(settingAttributes);

    return fileDetails.stream()
        .filter(fileDetail -> {
          String entityName = fileDetail.getEntityName();
          return nameSettingCategoryMap.containsKey(entityName)
              && nameSettingCategoryMap.get(entityName).contains(fileDetail.getType());
        })
        .map(YamlFileDetails::getYamlFilePath)
        .collect(Collectors.toSet());
  }

  private Map<String, List<SettingCategory>> mapOfNameAndSettingAttributes(List<SettingAttribute> settingAttributes) {
    Map<String, List<SettingCategory>> nameSettingCategoryMap = new HashMap<>();
    for (SettingAttribute settingAttribute : settingAttributes) {
      String name = settingAttribute.getName();
      List<SettingCategory> settingCategoryList = nameSettingCategoryMap.get(name);
      // if list does not exist create it
      if (settingCategoryList == null) {
        settingCategoryList = new ArrayList<>();
        settingCategoryList.add(settingAttribute.getCategory());
        handleTheCaseOfAzureArtifactStream(settingCategoryList, settingAttribute.getCategory());
        nameSettingCategoryMap.put(name, settingCategoryList);
      } else {
        // add if item is not already in list
        if (!settingCategoryList.contains(settingAttribute.getCategory())) {
          handleTheCaseOfAzureArtifactStream(settingCategoryList, settingAttribute.getCategory());
          settingCategoryList.add(settingAttribute.getCategory());
        }
      }
    }
    return nameSettingCategoryMap;
  }

  private void handleTheCaseOfAzureArtifactStream(List<SettingCategory> settingCategoryList, SettingCategory category) {
    if (AZURE_ARTIFACTS.equals(category)) {
      settingCategoryList.add(CONNECTOR);
    }
  }
}
