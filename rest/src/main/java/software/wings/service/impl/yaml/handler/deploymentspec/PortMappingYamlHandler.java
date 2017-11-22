package software.wings.service.impl.yaml.handler.deploymentspec;

import software.wings.beans.ErrorCode;
import software.wings.beans.container.PortMapping;
import software.wings.beans.container.PortMapping.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
public class PortMappingYamlHandler extends BaseYamlHandler<Yaml, PortMapping> {
  @Override
  public Yaml toYaml(PortMapping portMapping, String appId) {
    return Yaml.builder()
        .containerPort(portMapping.getContainerPort())
        .hostPort(portMapping.getHostPort())
        .loadBalancerPort(portMapping.isLoadBalancerPort())
        .build();
  }

  @Override
  public PortMapping upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  @Override
  public PortMapping updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  private PortMapping setWithYamlValues(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    return PortMapping.builder()
        .containerPort(yaml.getContainerPort())
        .hostPort(yaml.getHostPort())
        .loadBalancerPort(yaml.isLoadBalancerPort())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public PortMapping createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public PortMapping get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
}
