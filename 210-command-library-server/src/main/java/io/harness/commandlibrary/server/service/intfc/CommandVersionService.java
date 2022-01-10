/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.commandlibrary.server.service.intfc;

import io.harness.commandlibrary.server.beans.CommandArchiveContext;

import software.wings.beans.commandlibrary.CommandVersionEntity;

import java.util.List;
import java.util.Optional;

public interface CommandVersionService {
  Optional<CommandVersionEntity> getCommandVersionEntity(String commandStoreName, String commandName, String version);

  List<CommandVersionEntity> getAllVersionEntitiesForCommand(String commandStoreName, String commandName);

  String save(CommandVersionEntity commandVersionEntity);

  Optional<CommandVersionEntity> getEntityById(String commandVersionId);

  String createNewVersionFromArchive(CommandArchiveContext commandArchiveContext);
}
