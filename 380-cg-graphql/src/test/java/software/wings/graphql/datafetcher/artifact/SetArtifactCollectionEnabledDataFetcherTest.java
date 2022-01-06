/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.artifact;

import static io.harness.rule.OwnerRule.PRABU;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.AuthRuleGraphQL;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.mutation.artifact.QLSetArtifactCollectionEnabledPayload;
import software.wings.security.PermissionAttribute;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.AuthService;

import com.google.common.collect.ImmutableMap;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class SetArtifactCollectionEnabledDataFetcherTest extends AbstractDataFetcherTestBase {
  @Mock DataFetcherUtils utils;
  @Mock AuthRuleGraphQL authRuleInstrumentation;
  @Mock private AuthService authService;
  @Mock private ArtifactStreamService artifactStreamService;
  @InjectMocks
  @Spy
  SetArtifactCollectionEnabledDataFetcher setArtifactCollectionEnabledDataFetcher =
      new SetArtifactCollectionEnabledDataFetcher(artifactStreamService, authService);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void test_mutateAndFetchTrue() throws Exception {
    final DataFetchingEnvironment dataFetchingEnvironment = Mockito.mock(DataFetchingEnvironment.class);
    doReturn(ImmutableMap.of("artifactStreamId", "artifactStreamId", "artifactCollectionEnabled", "true"))
        .when(dataFetchingEnvironment)
        .getArguments();

    User user = testUtils.createUser(testUtils.createAccount());

    ArtifactStream artifactStream = DockerArtifactStream.builder()
                                        .appId("appId")
                                        .autoPopulate(true)
                                        .imageName("artifactImage")
                                        .uuid("artifactStreamId")
                                        .serviceId("serviceId")
                                        .metadataOnly(true)
                                        .build();

    doReturn("accountId").when(utils).getAccountId(dataFetchingEnvironment);
    doReturn(artifactStream).when(artifactStreamService).get("artifactStreamId");
    doNothing()
        .when(authService)
        .authorize("accountId", artifactStream.getUuid(), artifactStream.getServiceId(), user,
            asList(new PermissionAttribute(PermissionAttribute.ResourceType.SERVICE,
                PermissionAttribute.PermissionType.SERVICE, PermissionAttribute.Action.UPDATE)),
            true);
    doNothing().when(artifactStreamService).updateCollectionEnabled(artifactStream, true);
    final QLSetArtifactCollectionEnabledPayload artifactPayload =
        setArtifactCollectionEnabledDataFetcher.get(dataFetchingEnvironment);

    assertThat(artifactPayload.getMessage()).isEqualTo("Successfully set artifact collection enabled to true");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void test_mutateAndFetchFalse() throws Exception {
    final DataFetchingEnvironment dataFetchingEnvironment = Mockito.mock(DataFetchingEnvironment.class);
    doReturn(ImmutableMap.of("artifactStreamId", "artifactStreamId", "artifactCollectionEnabled", "false"))
        .when(dataFetchingEnvironment)
        .getArguments();

    User user = testUtils.createUser(testUtils.createAccount());

    ArtifactStream artifactStream = DockerArtifactStream.builder()
                                        .appId("appId")
                                        .autoPopulate(true)
                                        .imageName("artifactImage")
                                        .uuid("artifactStreamId")
                                        .serviceId("serviceId")
                                        .metadataOnly(true)
                                        .build();

    doReturn("accountId").when(utils).getAccountId(dataFetchingEnvironment);
    doReturn(artifactStream).when(artifactStreamService).get("artifactStreamId");
    doNothing()
        .when(authService)
        .authorize("accountId", artifactStream.getUuid(), artifactStream.getServiceId(), user,
            asList(new PermissionAttribute(PermissionAttribute.ResourceType.SERVICE,
                PermissionAttribute.PermissionType.SERVICE, PermissionAttribute.Action.UPDATE)),
            true);
    doNothing().when(artifactStreamService).updateCollectionEnabled(artifactStream, false);
    final QLSetArtifactCollectionEnabledPayload artifactPayload =
        setArtifactCollectionEnabledDataFetcher.get(dataFetchingEnvironment);

    assertThat(artifactPayload.getMessage()).isEqualTo("Successfully set artifact collection enabled to false");
  }
}
