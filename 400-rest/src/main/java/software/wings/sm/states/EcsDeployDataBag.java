/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceElement;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Service;

import java.util.List;
import lombok.Builder;
import lombok.Data;

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
