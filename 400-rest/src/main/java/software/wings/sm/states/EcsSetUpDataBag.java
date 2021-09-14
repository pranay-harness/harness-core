/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states;

import io.harness.k8s.model.ImageDetails;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsServiceSpecification;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsSetUpDataBag {
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
