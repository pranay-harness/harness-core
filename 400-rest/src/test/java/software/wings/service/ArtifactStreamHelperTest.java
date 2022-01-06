/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.utils.RepositoryFormat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ArtifactStreamHelperTest extends WingsBaseTest {
  @Inject ManagerExpressionEvaluator managerExpressionEvaluator;
  @Inject ArtifactStreamHelper artifactStreamHelper;

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testResolveArtifactStreamRuntimeValues() {
    assertThatThrownBy(
        ()
            -> artifactStreamHelper.resolveArtifactStreamRuntimeValues(
                NexusArtifactStream.builder()
                    .repositoryFormat(RepositoryFormat.maven.name())
                    .jobname("${jobName}")
                    .build(),
                ImmutableMap.<String, Object>builder().put("invalid", "someValue").put("buildNumber", "1").build()))
        .isInstanceOf(InvalidRequestException.class);

    assertThatThrownBy(
        ()
            -> artifactStreamHelper.resolveArtifactStreamRuntimeValues(
                NexusArtifactStream.builder()
                    .repositoryFormat(RepositoryFormat.maven.name())
                    .jobname("jobName")
                    .groupId("${groupId}")
                    .build(),
                ImmutableMap.<String, Object>builder().put("invalid", "someValue").put("buildNumber", "1").build()))
        .isInstanceOf(InvalidRequestException.class);

    assertThatThrownBy(
        ()
            -> artifactStreamHelper.resolveArtifactStreamRuntimeValues(
                NexusArtifactStream.builder()
                    .repositoryFormat(RepositoryFormat.maven.name())
                    .jobname("jobName")
                    .groupId("groupId")
                    .extension("${extension}")
                    .build(),
                ImmutableMap.<String, Object>builder().put("invalid", "someValue").put("buildNumber", "1").build()))
        .isInstanceOf(InvalidRequestException.class);

    assertThatThrownBy(
        ()
            -> artifactStreamHelper.resolveArtifactStreamRuntimeValues(
                NexusArtifactStream.builder()
                    .repositoryFormat(RepositoryFormat.maven.name())
                    .jobname("jobName")
                    .groupId("groupId")
                    .extension("extension")
                    .classifier("${classifier}")
                    .build(),
                ImmutableMap.<String, Object>builder().put("invalid", "someValue").put("buildNumber", "1").build()))
        .isInstanceOf(InvalidRequestException.class);

    assertThatThrownBy(
        ()
            -> artifactStreamHelper.resolveArtifactStreamRuntimeValues(
                NexusArtifactStream.builder()
                    .repositoryFormat(RepositoryFormat.maven.name())
                    .jobname("jobName")
                    .groupId("groupId")
                    .extension("extension")
                    .classifier("classifier")
                    .artifactPaths(Lists.newArrayList("path1", "${artifactPath}"))
                    .build(),
                ImmutableMap.<String, Object>builder().put("invalid", "someValue").put("buildNumber", "1").build()))
        .isInstanceOf(InvalidRequestException.class);

    assertThatThrownBy(
        ()
            -> artifactStreamHelper.resolveArtifactStreamRuntimeValues(
                NexusArtifactStream.builder()
                    .repositoryFormat(RepositoryFormat.nuget.name())
                    .jobname("${jobName}")
                    .build(),
                ImmutableMap.<String, Object>builder().put("invalid", "someValue").put("buildNumber", "1").build()))
        .isInstanceOf(InvalidRequestException.class);

    assertThatThrownBy(
        ()
            -> artifactStreamHelper.resolveArtifactStreamRuntimeValues(
                NexusArtifactStream.builder()
                    .repositoryFormat(RepositoryFormat.npm.name())
                    .jobname("jobName")
                    .packageName("${packageName}")
                    .build(),
                ImmutableMap.<String, Object>builder().put("invalid", "someValue").put("buildNumber", "1").build()))
        .isInstanceOf(InvalidRequestException.class);

    NexusArtifactStream maven = NexusArtifactStream.builder()
                                    .repositoryFormat(RepositoryFormat.maven.name())
                                    .jobname("${jobName}")
                                    .groupId("${groupId}")
                                    .extension("${extension}")
                                    .classifier("${classifier}")
                                    .artifactPaths(Lists.newArrayList("path1", "${artifactPath}"))
                                    .build();
    artifactStreamHelper.resolveArtifactStreamRuntimeValues(maven,
        ImmutableMap.<String, Object>builder()
            .put("invalid", "someValue")
            .put("jobName", "job")
            .put("groupId", "group")
            .put("extension", "ext")
            .put("classifier", "class")
            .put("artifactPath", "hello")
            .put("buildNumber", "1")
            .build());
    assertThat(maven.getJobname()).isEqualTo("job");
    assertThat(maven.getGroupId()).isEqualTo("group");
    assertThat(maven.getExtension()).isEqualTo("ext");
    assertThat(maven.getClassifier()).isEqualTo("class");
    assertThat(maven.getArtifactPaths()).isEqualTo(Lists.newArrayList("path1", "hello"));

    NexusArtifactStream npm = NexusArtifactStream.builder()
                                  .repositoryFormat(RepositoryFormat.npm.name())
                                  .jobname("${jobName}")
                                  .packageName("${packageName}")
                                  .build();
    artifactStreamHelper.resolveArtifactStreamRuntimeValues(npm,
        ImmutableMap.<String, Object>builder()
            .put("packageName", "package")
            .put("jobName", "job")
            .put("buildNumber", "1")
            .build());
    assertThat(npm.getJobname()).isEqualTo("job");
    assertThat(npm.getPackageName()).isEqualTo("package");
  }
}
