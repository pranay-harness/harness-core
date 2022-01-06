/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateRequest;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class ArtifactDelegateRequestUtils {
  public GcrArtifactDelegateRequest getGcrDelegateRequest(String imagePath, String tag, String tagRegex,
      List<String> tagsList, String registryHostname, String connectorRef, GcpConnectorDTO gcpConnectorDTO,
      List<EncryptedDataDetail> encryptedDataDetails, ArtifactSourceType sourceType) {
    return GcrArtifactDelegateRequest.builder()
        .imagePath(trim(imagePath))
        .tag(trim(tag))
        .tagRegex(trim(tagRegex))
        .tagsList(tagsList)
        .registryHostname(trim(registryHostname))
        .connectorRef(connectorRef)
        .gcpConnectorDTO(gcpConnectorDTO)
        .encryptedDataDetails(encryptedDataDetails)
        .sourceType(sourceType)
        .build();
  }
  public EcrArtifactDelegateRequest getEcrDelegateRequest(String imagePath, String tag, String tagRegex,
      List<String> tagsList, String region, String connectorRef, AwsConnectorDTO awsConnectorDTO,
      List<EncryptedDataDetail> encryptedDataDetails, ArtifactSourceType sourceType) {
    return EcrArtifactDelegateRequest.builder()
        .imagePath(trim(imagePath))
        .tag(trim(tag))
        .tagRegex(trim(tagRegex))
        .tagsList(tagsList)
        .region(trim(region))
        .connectorRef(connectorRef)
        .awsConnectorDTO(awsConnectorDTO)
        .encryptedDataDetails(encryptedDataDetails)
        .sourceType(sourceType)
        .build();
  }
  public DockerArtifactDelegateRequest getDockerDelegateRequest(String imagePath, String tag, String tagRegex,
      List<String> tagsList, String connectorRef, DockerConnectorDTO dockerConnectorDTO,
      List<EncryptedDataDetail> encryptedDataDetails, ArtifactSourceType sourceType) {
    return DockerArtifactDelegateRequest.builder()
        .imagePath(trim(imagePath))
        .tag(trim(tag))
        .tagRegex(trim(tagRegex))
        .tagsList(tagsList)
        .connectorRef(connectorRef)
        .dockerConnectorDTO(dockerConnectorDTO)
        .encryptedDataDetails(encryptedDataDetails)
        .sourceType(sourceType)
        .build();
  }
  private String trim(String str) {
    return str == null ? null : str.trim();
  }
}
