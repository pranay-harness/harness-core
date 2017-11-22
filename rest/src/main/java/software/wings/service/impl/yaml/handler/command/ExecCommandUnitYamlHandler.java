package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ExecCommandUnit.Yaml;
import software.wings.beans.command.ExecCommandUnit.Yaml.Builder;
import software.wings.beans.command.TailFilePatternEntry;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Util;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rktummala on 11/13/17
 */
public class ExecCommandUnitYamlHandler extends SshCommandUnitYamlHandler<Yaml, ExecCommandUnit, Builder> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.anYaml();
  }

  @Override
  protected ExecCommandUnit getCommandUnit() {
    return new ExecCommandUnit();
  }

  @Override
  public ExecCommandUnit createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    ExecCommandUnit execCommandUnit = super.createFromYaml(changeContext, changeSetContext);
    return setWithYamlValues(changeContext, execCommandUnit);
  }

  @Override
  public Yaml toYaml(ExecCommandUnit bean, String appId) {
    Yaml yaml = super.toYaml(bean, appId);
    yaml.setCommand(bean.getCommandString());
    yaml.setWorkingDirectory(bean.getCommandPath());

    yaml.setFilePatternEntryList(convertToYaml(bean.getTailPatterns()));
    return yaml;
  }

  @Override
  public ExecCommandUnit upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  private List<TailFilePatternEntry.Yaml> convertToYaml(List<TailFilePatternEntry> patternEntryList) {
    if (Util.isEmpty(patternEntryList)) {
      return Collections.emptyList();
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
      return Collections.emptyList();
    }

    return patternEntryYamlList.stream()
        .map(yamlEntry
            -> TailFilePatternEntry.Builder.aTailFilePatternEntry()
                   .withFilePath(yamlEntry.getFilePath())
                   .withPattern(yamlEntry.getSearchPattern())
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  public ExecCommandUnit updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    ExecCommandUnit execCommandUnit = super.updateFromYaml(changeContext, changeSetContext);
    return setWithYamlValues(changeContext, execCommandUnit);
  }

  private ExecCommandUnit setWithYamlValues(ChangeContext<Yaml> changeContext, ExecCommandUnit execCommandUnit) {
    Yaml yaml = changeContext.getYaml();
    execCommandUnit.setCommandString(yaml.getCommand());
    execCommandUnit.setCommandPath(yaml.getWorkingDirectory());
    execCommandUnit.setTailPatterns(convertToBean(yaml.getFilePatternEntryList()));
    return execCommandUnit;
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return super.validate(changeContext, changeSetContext);
  }
}
