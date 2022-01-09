/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.stream.BoundedInputStream;
import io.harness.validation.Create;
import io.harness.validation.Update;

import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.service.intfc.ownership.OwnedByHost;
import software.wings.service.intfc.ownership.OwnedByService;

import java.io.File;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

/**
 * Created by anubhaw on 4/25/16.
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface ConfigService extends OwnedByService, OwnedByHost, OwnedByEnvironment {
  /**
   * List.
   *
   * @param request the request
   * @return the page response
   */
  PageResponse<ConfigFile> list(PageRequest<ConfigFile> request);

  /**
   * Save.
   *
   * @param configFile  the config file
   * @param inputStream the input stream
   * @return the string
   */
  @ValidationGroups(Create.class) String save(@Valid ConfigFile configFile, BoundedInputStream inputStream);

  /**
   * Validate and compute relative path string.
   *
   * @param relativePath the relative path
   * @return the string
   */
  String validateAndResolveFilePath(String relativePath);

  /**
   * Gets the.
   *
   * @param appId            the app id
   * @param configId         the config id
   * @return the config file
   */
  ConfigFile get(@NotEmpty String appId, @NotEmpty String configId);

  /**
   * Gets the.
   *
   * @param appId            the app id
   * @param entityId         the entity id
   * @param entityType         the entity type
   * @return the config file
   */
  ConfigFile get(
      @NotEmpty String appId, @NotEmpty String entityId, EntityType entityType, @NotEmpty String relativeFilePath);

  /**
   * Download file.
   *
   * @param appId    the app id
   * @param configId the config id
   * @param version  the version
   * @return the file
   */
  File download(String appId, String configId, Integer version);

  /**
   * delegate call to download the file
   * @param appId
   * @param fileId
   * @param activityId
   * @return
   */
  File downloadForActivity(String appId, String fileId, String activityId);

  /**
   * Update.
   *
   * @param configFile  the config file
   * @param inputStream the input stream
   */
  @ValidationGroups(Update.class) void update(@Valid ConfigFile configFile, BoundedInputStream inputStream);

  /**
   * Delete.
   *
   * @param appId    the app id
   * @param configId the config id
   */
  void delete(@NotEmpty String appId, @NotEmpty String configId);

  /**
   * Delete based on name.
   * @param appId
   * @param entityId
   * @param entityType
   * @param configFileName
   */
  void delete(@NotEmpty String appId, @NotEmpty String entityId, @NotNull EntityType entityType,
      @NotEmpty String configFileName);

  void delete(@NotEmpty String appId, @NotEmpty String entityId, @NotNull EntityType entityType,
      @NotEmpty String configFileName, boolean syncFromGit);

  /**
   * Gets the config files for entity.
   *
   * @param appId      the app id
   * @param templateId the template id
   * @param entityId   the entity id
   * @return the config files for entity
   */
  List<ConfigFile> getConfigFilesForEntity(String appId, String templateId, String entityId, String envId);

  List<ConfigFile> getConfigFileOverridesForEnv(String appId, String envId);

  /**
   * Gets the config files for entity.
   *
   * @param appId      the app id
   * @param templateId the template id
   * @param entityId   the entity id
   * @return the config files for entity
   */
  List<ConfigFile> getConfigFilesForEntity(String appId, String templateId, String entityId);

  /**
   * Gets config file by template.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param serviceTemplateId the service template
   * @return the config file by template
   */
  List<ConfigFile> getConfigFileByTemplate(String appId, String envId, String serviceTemplateId);

  /**
   * Download file.
   *
   * @param appId    the app id
   * @param configId the config id
   * @return the file
   */
  File download(String appId, String configId);

  byte[] getFileContent(String appId, ConfigFile configFile);

  /**
   * Delete by entity id.
   *
   * @param appId      the app id
   * @param templateId the template id
   * @param entityId   the service id
   */
  void deleteByEntityId(String appId, String templateId, String entityId);

  /**
   * Delete by template id.
   *
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   */
  void deleteByTemplateId(String appId, String serviceTemplateId);

  /**
   * Delete by entity id.
   *
   * @param appId    the app id
   * @param entityId the entity id
   */
  void deleteByEntityId(String appId, String entityId);

  /**
   * Gets the config file for entity by relative file path.
   * @param appId
   * @param templateId
   * @param entityId
   * @param envId
   * @param relativeFilePath
   * @return
   */
  ConfigFile getConfigFileForEntityByRelativeFilePath(
      String appId, String templateId, String entityId, String envId, String relativeFilePath);
}
