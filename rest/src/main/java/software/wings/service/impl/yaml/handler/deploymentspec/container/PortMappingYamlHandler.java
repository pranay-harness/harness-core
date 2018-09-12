package software.wings.service.impl.yaml.handler.deploymentspec.container;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.container.PortMapping;
import software.wings.beans.container.PortMapping.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class PortMappingYamlHandler extends BaseYamlHandler<Yaml, PortMapping> {
  @Override
  public Yaml toYaml(PortMapping portMapping, String appId) {
    return Yaml.builder().containerPort(portMapping.getContainerPort()).hostPort(portMapping.getHostPort()).build();
  }

  @Override
  public PortMapping upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
  }

  private PortMapping toBean(ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();

    return PortMapping.builder().containerPort(yaml.getContainerPort()).hostPort(yaml.getHostPort()).build();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public PortMapping get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
