/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.artifact.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.EcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactDelegateRequestUtils;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateRequest;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class ArtifactConfigToDelegateReqMapper {
  public DockerArtifactDelegateRequest getDockerDelegateRequest(DockerHubArtifactConfig artifactConfig,
      DockerConnectorDTO connectorDTO, List<EncryptedDataDetail> encryptedDataDetails, String connectorRef) {
    // If both are empty, regex is latest among all docker artifacts.
    String tagRegex = artifactConfig.getTagRegex() != null ? artifactConfig.getTagRegex().getValue() : "";
    String tag = artifactConfig.getTag() != null ? artifactConfig.getTag().getValue() : "";
    if (EmptyPredicate.isEmpty(tag) && EmptyPredicate.isEmpty(tagRegex)) {
      tagRegex = "\\*";
    }
    return ArtifactDelegateRequestUtils.getDockerDelegateRequest(artifactConfig.getImagePath().getValue(), tag,
        tagRegex, null, connectorRef, connectorDTO, encryptedDataDetails, ArtifactSourceType.DOCKER_REGISTRY);
  }

  public GcrArtifactDelegateRequest getGcrDelegateRequest(GcrArtifactConfig gcrArtifactConfig,
      GcpConnectorDTO gcpConnectorDTO, List<EncryptedDataDetail> encryptedDataDetails, String connectorRef) {
    // If both are empty, regex is latest among all gcr artifacts.
    String tagRegex = gcrArtifactConfig.getTagRegex() != null ? gcrArtifactConfig.getTagRegex().getValue() : "";
    String tag = gcrArtifactConfig.getTag() != null ? gcrArtifactConfig.getTag().getValue() : "";
    if (EmptyPredicate.isEmpty(tag) && EmptyPredicate.isEmpty(tagRegex)) {
      tagRegex = "\\*";
    }
    return ArtifactDelegateRequestUtils.getGcrDelegateRequest(gcrArtifactConfig.getImagePath().getValue(), tag,
        tagRegex, null, gcrArtifactConfig.getRegistryHostname().getValue(), connectorRef, gcpConnectorDTO,
        encryptedDataDetails, ArtifactSourceType.GCR);
  }

  public EcrArtifactDelegateRequest getEcrDelegateRequest(EcrArtifactConfig ecrArtifactConfig,
      AwsConnectorDTO awsConnectorDTO, List<EncryptedDataDetail> encryptedDataDetails, String connectorRef) {
    // If both are empty, regex is latest among all ecr artifacts.
    String tagRegex = ecrArtifactConfig.getTagRegex() != null ? ecrArtifactConfig.getTagRegex().getValue() : "";
    String tag = ecrArtifactConfig.getTag() != null ? ecrArtifactConfig.getTag().getValue() : "";
    if (EmptyPredicate.isEmpty(tag) && EmptyPredicate.isEmpty(tagRegex)) {
      tagRegex = "\\*";
    }
    return ArtifactDelegateRequestUtils.getEcrDelegateRequest(ecrArtifactConfig.getImagePath().getValue(), tag,
        tagRegex, null, ecrArtifactConfig.getRegion().getValue(), connectorRef, awsConnectorDTO, encryptedDataDetails,
        ArtifactSourceType.ECR);
  }
}
