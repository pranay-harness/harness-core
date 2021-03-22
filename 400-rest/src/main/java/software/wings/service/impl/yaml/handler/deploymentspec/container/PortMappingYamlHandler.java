package software.wings.service.impl.yaml.handler.deploymentspec.container;

import io.harness.eraro.ErrorCode;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;

import software.wings.beans.container.PortMapping;
import software.wings.beans.container.PortMappingYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class PortMappingYamlHandler extends BaseYamlHandler<PortMappingYaml, PortMapping> {
  @Override
  public PortMappingYaml toYaml(PortMapping portMapping, String appId) {
    return PortMappingYaml.builder()
        .containerPort(portMapping.getContainerPort())
        .hostPort(portMapping.getHostPort())
        .build();
  }

  @Override
  public PortMapping upsertFromYaml(ChangeContext<PortMappingYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
  }

  private PortMapping toBean(ChangeContext<PortMappingYaml> changeContext) {
    PortMappingYaml yaml = changeContext.getYaml();

    return PortMapping.builder().containerPort(yaml.getContainerPort()).hostPort(yaml.getHostPort()).build();
  }

  @Override
  public Class getYamlClass() {
    return PortMappingYaml.class;
  }

  @Override
  public PortMapping get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<PortMappingYaml> changeContext) {
    // Do nothing
  }
}
