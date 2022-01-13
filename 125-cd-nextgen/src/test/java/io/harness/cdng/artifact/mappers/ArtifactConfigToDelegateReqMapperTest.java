/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.mappers;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class ArtifactConfigToDelegateReqMapperTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetDockerDelegateRequest() {
    DockerHubArtifactConfig dockerHubArtifactConfig =
        DockerHubArtifactConfig.builder().imagePath(ParameterField.createValueField("IMAGE")).build();
    DockerConnectorDTO connectorDTO = DockerConnectorDTO.builder().build();
    List<EncryptedDataDetail> encryptedDataDetailList = Collections.emptyList();

    DockerArtifactDelegateRequest dockerDelegateRequest = ArtifactConfigToDelegateReqMapper.getDockerDelegateRequest(
        dockerHubArtifactConfig, connectorDTO, encryptedDataDetailList, "");

    assertThat(dockerDelegateRequest.getDockerConnectorDTO()).isEqualTo(connectorDTO);
    assertThat(dockerDelegateRequest.getEncryptedDataDetails()).isEqualTo(encryptedDataDetailList);
    assertThat(dockerDelegateRequest.getImagePath()).isEqualTo(dockerHubArtifactConfig.getImagePath().getValue());
    assertThat(dockerDelegateRequest.getSourceType()).isEqualTo(ArtifactSourceType.DOCKER_REGISTRY);
    assertThat(dockerDelegateRequest.getTagsList()).isNull();
    assertThat(dockerDelegateRequest.getTag()).isEqualTo("");
    assertThat(dockerDelegateRequest.getConnectorRef()).isEqualTo("");
    assertThat(dockerDelegateRequest.getTagRegex()).isEqualTo("\\*");
  }
}
