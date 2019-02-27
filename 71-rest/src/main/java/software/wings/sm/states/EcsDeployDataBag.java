package software.wings.sm.states;

import lombok.Builder;
import lombok.Data;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceElement;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@Builder
class EcsDeployDataBag {
  private String region;
  private Application app;
  private Environment env;
  private Service service;
  private AwsConfig awsConfig;
  private ContainerServiceElement containerElement;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private ContainerRollbackRequestElement rollbackElement;
  private EcsInfrastructureMapping ecsInfrastructureMapping;
}