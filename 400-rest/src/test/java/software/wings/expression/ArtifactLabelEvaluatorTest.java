/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.expression;

import static io.harness.rule.OwnerRule.HARSH;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.service.intfc.BuildSourceService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class ArtifactLabelEvaluatorTest extends WingsBaseTest {
  private ArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .uuid(ARTIFACT_STREAM_ID)
                                                    .accountId(ACCOUNT_ID)
                                                    .appId(APP_ID)
                                                    .settingId(SETTING_ID)
                                                    .imageName("wingsplugins/todolist")
                                                    .autoPopulate(true)
                                                    .serviceId(SERVICE_ID)
                                                    .build();
  @Mock private BuildSourceService buildSourceService;

  private String key = "foo";
  private String value = "abc";

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldGetLabelsforValidKey() {
    ArtifactLabelEvaluator artifactLabelEvaluator = ArtifactLabelEvaluator.builder()
                                                        .artifactStream(dockerArtifactStream)
                                                        .buildNo("abc")
                                                        .buildSourceService(buildSourceService)
                                                        .build();

    List<Map<String, String>> arrayList = new ArrayList<>();

    Map<String, String> labels = new HashMap<>();
    labels.put(key, value);
    arrayList.add(labels);

    when(buildSourceService.getLabels(any(), any())).thenReturn(arrayList);
    assertEquals(artifactLabelEvaluator.get("foo"), "abc");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldNotGetLabelsforInValidKey() {
    ArtifactLabelEvaluator artifactLabelEvaluator = ArtifactLabelEvaluator.builder()
                                                        .artifactStream(dockerArtifactStream)
                                                        .buildNo("abc")
                                                        .buildSourceService(buildSourceService)
                                                        .build();

    List<Map<String, String>> arrayList = new ArrayList<>();

    Map<String, String> labels = new HashMap<>();
    labels.put(key, value);
    arrayList.add(labels);

    when(buildSourceService.getLabels(any(), any())).thenReturn(arrayList);
    assertEquals(artifactLabelEvaluator.get("foo1"), "foo1");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldThrowExceptionforEmptyKeySet() {
    ArtifactLabelEvaluator artifactLabelEvaluator = ArtifactLabelEvaluator.builder()
                                                        .artifactStream(dockerArtifactStream)
                                                        .buildNo("abc")
                                                        .buildSourceService(buildSourceService)
                                                        .build();

    when(buildSourceService.getLabels(any(), any())).thenReturn(null);
    assertEquals(artifactLabelEvaluator.get("foo1"), "foo1");
  }
}
