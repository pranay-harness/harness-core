package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.command.ScpCommandUnit.Yaml;
import software.wings.beans.command.ScpCommandUnit.Yaml.Builder;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Util;

import java.util.List;

/**
 * @author rktummala on 11/13/17
 */
public class ScpCommandUnitYamlHandler extends SshCommandUnitYamlHandler<Yaml, ScpCommandUnit, Builder> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.anYaml();
  }

  @Override
  protected ScpCommandUnit getCommandUnit() {
    return new ScpCommandUnit();
  }

  @Override
  public ScpCommandUnit createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    ScpCommandUnit scpCommandUnit = super.createFromYaml(changeContext, changeSetContext);
    return setWithYamlValues(changeContext, scpCommandUnit);
  }

  @Override
  public Yaml toYaml(ScpCommandUnit bean, String appId) {
    Yaml yaml = super.toYaml(bean, appId);
    yaml.setDestinationDirectoryPath(bean.getDestinationDirectoryPath());
    String fileCategory = Util.getStringFromEnum(bean.getFileCategory());
    yaml.setSource(fileCategory);
    return yaml;
  }

  @Override
  public ScpCommandUnit updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    ScpCommandUnit scpCommandUnit = super.updateFromYaml(changeContext, changeSetContext);
    return setWithYamlValues(changeContext, scpCommandUnit);
  }

  private ScpCommandUnit setWithYamlValues(ChangeContext<Yaml> changeContext, ScpCommandUnit scpCommandUnit) {
    Yaml yaml = changeContext.getYaml();
    scpCommandUnit.setDestinationDirectoryPath(yaml.getDestinationDirectoryPath());
    ScpFileCategory scpFileCategory = Util.getEnumFromString(ScpFileCategory.class, yaml.getSource());
    scpCommandUnit.setFileCategory(scpFileCategory);
    return scpCommandUnit;
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    boolean validate = super.validate(changeContext, changeSetContext);
    Yaml yaml = changeContext.getYaml();
    if (validate) {
      return !(yaml.getDestinationDirectoryPath() == null || yaml.getSource() == null);
    }

    return validate;
  }
}
