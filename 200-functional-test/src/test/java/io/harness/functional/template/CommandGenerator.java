/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.template;

import io.harness.generator.AccountGenerator;
import io.harness.generator.GeneratorUtils;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.TemplateFolderGenerator;
import io.harness.generator.TemplateGalleryGenerator;

import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandEntity.CommandEntityBuilder;
import software.wings.beans.commandlibrary.CommandEntity.CommandEntityKeys;
import software.wings.beans.commandlibrary.CommandVersionEntity;
import software.wings.beans.commandlibrary.CommandVersionEntity.CommandVersionEntityBuilder;
import software.wings.beans.commandlibrary.CommandVersionEntity.CommandVersionsKeys;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateService;

import com.google.inject.Inject;
import io.github.benas.randombeans.api.EnhancedRandom;

public class CommandGenerator {
  @Inject AccountGenerator accountGenerator;
  @Inject TemplateGalleryGenerator templateGalleryGenerator;
  @Inject TemplateFolderGenerator templateFolderGenerator;
  @Inject WingsPersistence wingsPersistence;
  @Inject TemplateService templateService;

  public CommandEntity ensureCommandEntityWithNameAndCommandStoreName(
      Randomizer.Seed seed, OwnerManager.Owners owners, String commandName, String commandStoreName) {
    return ensureCommandEntity(
        seed, owners, CommandEntity.builder().name(commandName).commandStoreName(commandStoreName).build());
  }

  public CommandVersionEntity ensureCommandVersionEntity(
      Randomizer.Seed seed, OwnerManager.Owners owners, String version, String commandName, String commandStoreName) {
    return ensureCommandVersionEntity(seed, owners,
        CommandVersionEntity.builder()
            .commandStoreName(commandStoreName)
            .commandName(commandName)
            .version(version)
            .build());
  }

  public CommandEntity ensureCommandEntity(
      Randomizer.Seed seed, OwnerManager.Owners owners, CommandEntity commandEntity) {
    EnhancedRandom random = Randomizer.instance(seed);
    CommandEntityBuilder builder = CommandEntity.builder();
    if (commandEntity != null && commandEntity.getName() != null) {
      builder.name(commandEntity.getName());
    } else {
      builder.name(random.nextObject(String.class));
    }
    if (commandEntity != null && commandEntity.getCommandStoreName() != null) {
      builder.commandStoreName(commandEntity.getCommandStoreName());
    } else {
      builder.commandStoreName("HARNESS");
    }
    if (commandEntity != null && commandEntity.getDescription() != null) {
      builder.description(commandEntity.getDescription());
    }
    if (commandEntity != null && commandEntity.getLatestVersion() != null) {
      builder.latestVersion(commandEntity.getLatestVersion());
    }
    if (commandEntity != null && commandEntity.getImageUrl() != null) {
      builder.imageUrl(commandEntity.getImageUrl());
    }
    if (commandEntity != null && commandEntity.getRepoUrl() != null) {
      builder.repoUrl(commandEntity.getRepoUrl());
    }
    if (commandEntity != null && commandEntity.getTags() != null) {
      builder.tags(commandEntity.getTags());
    }
    if (commandEntity != null && commandEntity.getType() != null) {
      builder.type(commandEntity.getType());
    }

    CommandEntity existingCommandEntity = commandExists(builder.build());
    if (existingCommandEntity != null) {
      CommandEntity existing = wingsPersistence.get(CommandEntity.class, existingCommandEntity.getUuid());
      if (existing != null) {
        return existing;
      }
    }

    final CommandEntity finalCommand = builder.build();

    return GeneratorUtils.suppressDuplicateException(
        ()
            -> wingsPersistence.get(CommandEntity.class, wingsPersistence.save(finalCommand)),
        () -> commandExists(finalCommand));
  }

  public CommandVersionEntity ensureCommandVersionEntity(
      Randomizer.Seed seed, OwnerManager.Owners owners, CommandVersionEntity commandVersionEntity) {
    EnhancedRandom random = Randomizer.instance(seed);
    CommandEntity commandEntity = ensureCommandEntityWithNameAndCommandStoreName(
        seed, owners, commandVersionEntity.getCommandName(), commandVersionEntity.getCommandStoreName());
    CommandVersionEntityBuilder builder = CommandVersionEntity.builder();

    builder.commandName(commandEntity.getName());
    builder.commandStoreName(commandEntity.getCommandStoreName());

    if (commandVersionEntity != null && commandVersionEntity.getDescription() != null) {
      builder.description(commandEntity.getDescription());
    }

    if (commandVersionEntity != null && commandVersionEntity.getVariables() != null) {
      builder.variables(commandVersionEntity.getVariables());
    }
    if (commandVersionEntity != null && commandVersionEntity.getRepoUrl() != null) {
      builder.repoUrl(commandVersionEntity.getRepoUrl());
    }
    if (commandVersionEntity != null && commandVersionEntity.getTemplateObject() != null) {
      builder.templateObject(commandVersionEntity.getTemplateObject());
    } else {
      builder.templateObject(SshCommandTemplate.builder().commandUnits(null).commands(null).build());
    }
    if (commandVersionEntity != null && commandVersionEntity.getVersion() != null) {
      builder.version(commandVersionEntity.getVersion());
    } else {
      builder.version("1.0");
    }

    CommandVersionEntity existingCommandVersionEntity = commandVersionExists(builder.build());
    if (existingCommandVersionEntity != null) {
      CommandVersionEntity existing =
          wingsPersistence.get(CommandVersionEntity.class, existingCommandVersionEntity.getUuid());
      if (existing != null) {
        return existing;
      }
    }

    final CommandVersionEntity finalCommand = builder.build();

    return GeneratorUtils.suppressDuplicateException(
        ()
            -> wingsPersistence.get(CommandVersionEntity.class, wingsPersistence.save(finalCommand)),
        () -> commandVersionExists(finalCommand));
  }

  private CommandEntity commandExists(CommandEntity commandEntity) {
    return wingsPersistence.createQuery(CommandEntity.class)
        .filter(CommandEntityKeys.commandStoreName, commandEntity.getCommandStoreName())
        .filter(CommandEntityKeys.name, commandEntity.getName())
        .get();
  }

  private CommandVersionEntity commandVersionExists(CommandVersionEntity commandVersionEntity) {
    return wingsPersistence.createQuery(CommandVersionEntity.class)
        .filter(CommandVersionsKeys.commandStoreName, commandVersionEntity.getCommandStoreName())
        .filter(CommandVersionsKeys.commandName, commandVersionEntity.getCommandName())
        .filter(CommandVersionsKeys.version, commandVersionEntity.getVersion())
        .get();
  }
}
