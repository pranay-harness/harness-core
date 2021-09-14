/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class AwsAmiTrafficShiftAlbData {
  private Artifact artifact;
  private Application app;
  private Service service;
  private Environment env;
  private AwsConfig awsConfig;
  private AwsAmiInfrastructureMapping infrastructureMapping;
  private List<EncryptedDataDetail> awsEncryptedDataDetails;
  private String region;
  private String serviceId;
  private EmbeddedUser currentUser;
}
