/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
