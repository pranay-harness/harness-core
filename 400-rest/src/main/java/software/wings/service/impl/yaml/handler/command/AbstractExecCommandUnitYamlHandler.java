/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.command;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_COMMAND_PATH;
import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_COMMAND_STRING;
import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_COMMAND_TYPE;
import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_TAIL_FILES;
import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_TAIL_PATTERNS;

import static java.util.stream.Collectors.toList;

import io.harness.shell.ScriptType;

import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ExecCommandUnitAbstractYaml;
import software.wings.beans.command.TailFilePatternEntry;
import software.wings.beans.yaml.ChangeContext;
import software.wings.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author rktummala on 11/13/17
 */
public abstract class AbstractExecCommandUnitYamlHandler<Y extends ExecCommandUnitAbstractYaml, B
                                                             extends ExecCommandUnit>
    extends SshCommandUnitYamlHandler<Y, B> {
  @Override
  protected void toYaml(Y yaml, B bean) {
    super.toYaml(yaml, bean);
    yaml.setScriptType(bean.getScriptType().name());
    yaml.setCommand(bean.getCommandString());
    yaml.setWorkingDirectory(bean.getCommandPath());
    yaml.setFilePatternEntryList(convertToYaml(bean.getTailPatterns()));
  }

  private List<TailFilePatternEntry.Yaml> convertToYaml(List<TailFilePatternEntry> patternEntryList) {
    if (isEmpty(patternEntryList)) {
      return null;
    }

    return patternEntryList.stream()
        .filter(Objects::nonNull)
        .map(entry
            -> TailFilePatternEntry.Yaml.Builder.anYaml()
                   .withFilePath(entry.getFilePath())
                   .withSearchPattern(entry.getPattern())
                   .build())
        .collect(toList());
  }

  protected List<TailFilePatternEntry> convertToBean(List<TailFilePatternEntry.Yaml> patternEntryYamlList) {
    if (isEmpty(patternEntryYamlList)) {
      return null;
    }

    return patternEntryYamlList.stream()
        .filter(Objects::nonNull)
        .map(yamlEntry
            -> TailFilePatternEntry.Builder.aTailFilePatternEntry()
                   .withFilePath(yamlEntry.getFilePath())
                   .withPattern(yamlEntry.getSearchPattern())
                   .build())
        .collect(toList());
  }

  @Override
  protected B toBean(ChangeContext<Y> changeContext) {
    B bean = super.toBean(changeContext);
    Y yaml = changeContext.getYaml();
    ScriptType scriptType = isEmpty(yaml.getScriptType())
        ? ScriptType.BASH
        : Utils.getEnumFromString(ScriptType.class, yaml.getScriptType());
    bean.setScriptType(scriptType);
    bean.setCommandString(yaml.getCommand());
    bean.setCommandPath(yaml.getWorkingDirectory());
    bean.setTailPatterns(convertToBean(yaml.getFilePatternEntryList()));
    return bean;
  }

  @Override
  public B toBean(AbstractCommandUnit.Yaml yaml) {
    B bean = super.toBean(yaml);
    final ExecCommandUnitAbstractYaml execYaml = (ExecCommandUnitAbstractYaml) yaml;
    ScriptType scriptType = isEmpty(execYaml.getScriptType())
        ? ScriptType.BASH
        : Utils.getEnumFromString(ScriptType.class, execYaml.getScriptType());
    bean.setScriptType(scriptType);
    bean.setCommandString(execYaml.getCommand());
    bean.setCommandPath(execYaml.getWorkingDirectory());
    bean.setDeploymentType(execYaml.getDeploymentType());
    return bean;
  }

  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<Y> changeContext) {
    Y yaml = changeContext.getYaml();

    Map<String, Object> nodeProperties = super.getNodeProperties(changeContext);

    nodeProperties.put(NODE_PROPERTY_COMMAND_PATH, yaml.getWorkingDirectory());
    nodeProperties.put(NODE_PROPERTY_COMMAND_STRING, yaml.getCommand());
    nodeProperties.put(NODE_PROPERTY_COMMAND_TYPE, yaml.getCommandUnitType());

    List<TailFilePatternEntry.Yaml> filePatternEntryList = yaml.getFilePatternEntryList();
    if (isNotEmpty(filePatternEntryList)) {
      List<String> patternList = filePatternEntryList.stream()
                                     .filter(Objects::nonNull)
                                     .map(TailFilePatternEntry.Yaml::getSearchPattern)
                                     .collect(toList());
      nodeProperties.put(NODE_PROPERTY_TAIL_FILES, true);
      nodeProperties.put(NODE_PROPERTY_TAIL_PATTERNS, patternList);
    }

    return nodeProperties;
  }
}
