/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.helpers.ext.ecr;

import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.BuildDetails.BuildDetailsMetadataKeys;
import software.wings.service.impl.AwsApiHelperService;
import software.wings.service.intfc.aws.delegate.AwsEcrHelperServiceDelegate;
import software.wings.service.mappers.artifact.ArtifactConfigMapper;
import software.wings.service.mappers.artifact.AwsConfigToInternalMapper;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.ImageIdentifier;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import com.amazonaws.services.ecr.model.Repository;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class EcrServiceTest extends WingsBaseTest {
  @Mock private AwsEcrHelperServiceDelegate ecrServiceDelegate;
  @Inject @InjectMocks private EcrService ecrService;
  @Mock private AwsApiHelperService awsApiHelperService;

  private AwsConfig awsConfig = AwsConfig.builder().build();

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void shouldGetLabels() {
    when(awsApiHelperService.fetchLabels(eq(AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig)), eq("imageName"),
             eq(Regions.US_EAST_1.getName()), anyListOf(String.class)))
        .thenReturn(ImmutableMap.<String, String>builder().put("key1", "val1").build());
    assertThat(ecrService.getLabels(AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig), "imageName",
                   Regions.US_EAST_1.getName(), Lists.newArrayList("tag1")))
        .hasSize(1)
        .isEqualTo(Collections.singletonList(ImmutableMap.<String, String>builder().put("key1", "val1").build()));
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void shouldGetBuilds() {
    String region = Regions.US_EAST_1.getName();
    when(ecrServiceDelegate.getEcrImageUrl(awsConfig, null, region, "imageName")).thenReturn("imageUrl");
    ListImagesResult imagesResult = new ListImagesResult();
    imagesResult.setNextToken(null);
    imagesResult.setImageIds(Lists.newArrayList(null, buildImageIdentifier(null), buildImageIdentifier("latest"),
        buildImageIdentifier("v2"), buildImageIdentifier("v1")));
    when(awsApiHelperService.listEcrImages(AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig), region,
             new ListImagesRequest().withRepositoryName("imageName")))
        .thenReturn(imagesResult);
    assertThat(
        ecrService.getBuilds(AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig), null, region, "imageName", 10)
            .stream()
            .map(ArtifactConfigMapper::toBuildDetails)
            .collect(Collectors.toList()))
        .hasSize(3)
        .isEqualTo(Lists.newArrayList(buildBuildDetails("latest"), buildBuildDetails("v1"), buildBuildDetails("v2")));
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testListEcrRegistry() {
    DescribeRepositoriesResult describeRepositoriesResult = new DescribeRepositoriesResult();
    describeRepositoriesResult.setRepositories(Lists.newArrayList("repo1", "repo2")
                                                   .stream()
                                                   .map(repoName -> {
                                                     Repository repo = new Repository();
                                                     repo.setRepositoryName(repoName);
                                                     return repo;
                                                   })
                                                   .collect(Collectors.toList()));
    describeRepositoriesResult.setNextToken(null);
    when(awsApiHelperService.listRepositories(AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig),
             new DescribeRepositoriesRequest(), Regions.US_EAST_1.getName()))
        .thenReturn(describeRepositoriesResult);
    assertThat(ecrService.listEcrRegistry(
                   AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig), Regions.US_EAST_1.getName()))
        .containsExactly("repo1", "repo2");
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testVerifyRepository() {
    DescribeRepositoriesResult describeRepositoriesResult = new DescribeRepositoriesResult();
    describeRepositoriesResult.setRepositories(Lists.newArrayList("repo1", "repo2")
                                                   .stream()
                                                   .map(repoName -> {
                                                     Repository repo = new Repository();
                                                     repo.setRepositoryName(repoName);
                                                     return repo;
                                                   })
                                                   .collect(Collectors.toList()));
    describeRepositoriesResult.setNextToken(null);
    when(awsApiHelperService.listRepositories(AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig),
             new DescribeRepositoriesRequest(), Regions.US_EAST_1.getName()))
        .thenReturn(describeRepositoriesResult);
    assertThat(ecrService.verifyRepository(
                   AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig), Regions.US_EAST_1.getName(), "repo1"))
        .isTrue();
  }

  private ImageIdentifier buildImageIdentifier(String tag) {
    ImageIdentifier imageIdentifier = new ImageIdentifier();
    imageIdentifier.setImageTag(tag);
    return imageIdentifier;
  }

  private BuildDetails buildBuildDetails(String tag) {
    Map<String, String> metadata = new HashMap();
    metadata.put(BuildDetailsMetadataKeys.image, "imageUrl:" + tag);
    metadata.put(BuildDetailsMetadataKeys.tag, tag);
    return aBuildDetails().withNumber(tag).withMetadata(metadata).withUiDisplayName("Tag# " + tag).build();
  }
}
