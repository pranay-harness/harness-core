/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.service.impl;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.repositories.artifact.ArtifactSourceDao;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ArtifactSourceServiceImplTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private ArtifactSourceDao artifactSourceDao;
  @Mock private ArtifactSource artifactSource;
  @InjectMocks private ArtifactSourceServiceImpl artifactSourceService;

  @Before
  public void setUp() throws Exception {
    doReturn("ACCOUNT_ID").when(artifactSource).getAccountId();
    doReturn("UNIQUE_HASH").when(artifactSource).getUniqueHash();
    doReturn("UUID").when(artifactSource).getUuid();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldSaveAndGetArtifactSource() {
    doReturn(null).when(artifactSourceDao).getArtifactStreamByHash("ACCOUNT_ID", "UNIQUE_HASH");
    doReturn(artifactSource).when(artifactSourceDao).create(artifactSource);
    ArtifactSource artifactSourceResult = artifactSourceService.saveOrGetArtifactStream(artifactSource);
    verify(artifactSourceDao).create(artifactSource);
    assertThat(artifactSourceResult).isEqualTo(artifactSource);

    doReturn(artifactSource).when(artifactSourceDao).getArtifactStreamByHash("ACCOUNT_ID", "UNIQUE_HASH");
    artifactSourceResult = artifactSourceService.getArtifactStreamByHash("ACCOUNT_ID", "UNIQUE_HASH");
    assertThat(artifactSourceResult).isEqualTo(artifactSource);

    doReturn(artifactSource).when(artifactSourceDao).get("ACCOUNT_ID", "UUID");
    artifactSourceResult = artifactSourceService.get("ACCOUNT_ID", "UUID");
    assertThat(artifactSourceResult).isEqualTo(artifactSource);
  }
}
