package software.wings.service.impl.yaml.handler.command;

import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_COMMAND_PATH;
import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_COMMAND_STRING;
import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_COMMAND_TYPE;
import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_TAIL_FILES;
import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_TAIL_PATTERNS;

import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ExecCommandUnit.AbstractYaml;
import software.wings.beans.command.TailFilePatternEntry;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rktummala on 11/13/17
 */
public abstract class AbstractExecCommandUnitYamlHandler<Y extends AbstractYaml, B extends ExecCommandUnit>
    extends SshCommandUnitYamlHandler<Y, B> {
  protected void toYaml(Y yaml, B bean) {
    super.toYaml(yaml, bean);
    yaml.setCommand(bean.getCommandString());
    yaml.setWorkingDirectory(bean.getCommandPath());
    yaml.setFilePatternEntryList(convertToYaml(bean.getTailPatterns()));
  }

  private List<TailFilePatternEntry.Yaml> convertToYaml(List<TailFilePatternEntry> patternEntryList) {
    if (Util.isEmpty(patternEntryList)) {
      return null;
    }

    return patternEntryList.stream()
        .map(entry
            -> TailFilePatternEntry.Yaml.Builder.anYaml()
                   .withFilePath(entry.getFilePath())
                   .withSearchPattern(entry.getPattern())
                   .build())
        .collect(Collectors.toList());
  }

  private List<TailFilePatternEntry> convertToBean(List<TailFilePatternEntry.Yaml> patternEntryYamlList) {
    if (Util.isEmpty(patternEntryYamlList)) {
      return null;
    }

    return patternEntryYamlList.stream()
        .map(yamlEntry
            -> TailFilePatternEntry.Builder.aTailFilePatternEntry()
                   .withFilePath(yamlEntry.getFilePath())
                   .withPattern(yamlEntry.getSearchPattern())
                   .build())
        .collect(Collectors.toList());
  }

  protected B toBean(ChangeContext<Y> changeContext) throws HarnessException {
    B bean = super.toBean(changeContext);
    Y yaml = changeContext.getYaml();
    bean.setCommandString(yaml.getCommand());
    bean.setCommandPath(yaml.getWorkingDirectory());
    bean.setTailPatterns(convertToBean(yaml.getFilePatternEntryList()));
    return bean;
  }

  @Override
  public boolean validate(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) {
    return super.validate(changeContext, changeSetContext);
  }

  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<Y> changeContext) {
    Y yaml = changeContext.getYaml();

    Map<String, Object> nodeProperties = super.getNodeProperties(changeContext);

    nodeProperties.put(NODE_PROPERTY_COMMAND_PATH, yaml.getWorkingDirectory());
    nodeProperties.put(NODE_PROPERTY_COMMAND_STRING, yaml.getCommand());
    nodeProperties.put(NODE_PROPERTY_COMMAND_TYPE, yaml.getCommandUnitType());

    List<TailFilePatternEntry.Yaml> filePatternEntryList = yaml.getFilePatternEntryList();
    if (!Util.isEmpty(filePatternEntryList)) {
      List<String> patternList = filePatternEntryList.stream()
                                     .map(filePatternEntry -> filePatternEntry.getSearchPattern())
                                     .collect(Collectors.toList());
      nodeProperties.put(NODE_PROPERTY_TAIL_FILES, true);
      nodeProperties.put(NODE_PROPERTY_TAIL_PATTERNS, patternList);
    }

    return nodeProperties;
  }
}
