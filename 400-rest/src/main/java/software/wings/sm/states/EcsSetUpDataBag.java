/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
