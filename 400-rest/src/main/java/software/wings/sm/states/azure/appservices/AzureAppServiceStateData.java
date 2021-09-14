/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.azure.appservices;

import io.harness.beans.EmbeddedUser;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.Application;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureAppServiceStateData {
  private Artifact artifact;
  private Application application;
  private Service service;
  private String serviceId;
  private Environment environment;
  private AzureConfig azureConfig;
  private List<EncryptedDataDetail> azureEncryptedDataDetails;
  private AzureWebAppInfrastructureMapping infrastructureMapping;
  private String resourceGroup;
  private String subscriptionId;
  private EmbeddedUser currentUser;
}
