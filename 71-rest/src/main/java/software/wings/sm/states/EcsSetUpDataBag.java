package software.wings.sm.states;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@Builder
class EcsSetUpDataBag {
  Service service;
  AwsConfig awsConfig;
  Environment environment;
  Application application;
  ImageDetails imageDetails;
  ContainerTask containerTask;
  int serviceSteadyStateTimeout;
  EcsServiceSpecification serviceSpecification;
  List<EncryptedDataDetail> encryptedDataDetails;
  EcsInfrastructureMapping ecsInfrastructureMapping;
}