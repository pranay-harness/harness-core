package io.harness.stateutils.buildstate;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.steps.stepinfo.DockerStepInfo;
import io.harness.beans.steps.stepinfo.ECRStepInfo;
import io.harness.beans.steps.stepinfo.GCRStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheGCSStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheS3StepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheGCSStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheS3StepInfo;
import io.harness.beans.steps.stepinfo.UploadToGCSStepInfo;
import io.harness.beans.steps.stepinfo.UploadToS3StepInfo;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PluginSettingUtilsTest extends CIExecutionTest {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetGCRStepInfoEnvVariables() {
    GCRStepInfo dockerStepInfo = GCRStepInfo.builder()
                                     .registry("gcr.io")
                                     .repo("harness")
                                     .tags(asList("tag1", "tag2"))
                                     .dockerfile("Dockerfile")
                                     .context("context")
                                     .target("target")
                                     .buildArgs(asList("arg1", "arg2"))
                                     .labels(Collections.singletonMap("label", "label1"))
                                     .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_REGISTRY", "gcr.io");
    expected.put("PLUGIN_REPO", "harness");
    expected.put("PLUGIN_TAGS", "tag1,tag2");
    expected.put("PLUGIN_DOCKERFILE", "Dockerfile");
    expected.put("PLUGIN_CONTEXT", "context");
    expected.put("PLUGIN_TARGET", "target");
    expected.put("PLUGIN_BUILD_ARGS", "arg1,arg2");
    expected.put("PLUGIN_CUSTOM_LABELS", "label=label1");
    Map<String, String> pluginCompatiblePublishStepEnvVariables =
        PluginSettingUtils.getPluginCompatibleEnvVariables(dockerStepInfo);
    assertThat(pluginCompatiblePublishStepEnvVariables).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetECRStepInfoStepEnvVariables() {
    ECRStepInfo dockerStepInfo = ECRStepInfo.builder()
                                     .registry("https://aws_account_id.dkr.ecr.region.amazonaws.com.")
                                     .repo("harness")
                                     .tags(asList("tag1", "tag2"))
                                     .dockerfile("Dockerfile")
                                     .context("context")
                                     .target("target")
                                     .buildArgs(asList("arg1", "arg2"))
                                     .labels(Collections.singletonMap("label", "label1"))
                                     .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_REGISTRY", "https://aws_account_id.dkr.ecr.region.amazonaws.com.");
    expected.put("PLUGIN_REPO", "harness");
    expected.put("PLUGIN_TAGS", "tag1,tag2");
    expected.put("PLUGIN_DOCKERFILE", "Dockerfile");
    expected.put("PLUGIN_CONTEXT", "context");
    expected.put("PLUGIN_TARGET", "target");
    expected.put("PLUGIN_BUILD_ARGS", "arg1,arg2");
    expected.put("PLUGIN_CUSTOM_LABELS", "label=label1");
    Map<String, String> pluginCompatiblePublishStepEnvVariables =
        PluginSettingUtils.getPluginCompatibleEnvVariables(dockerStepInfo);
    assertThat(pluginCompatiblePublishStepEnvVariables).isEqualTo(expected);
  }
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetDockerStepInfoEnvVariables() {
    DockerStepInfo dockerStepInfo = DockerStepInfo.builder()
                                        .repo("harness")
                                        .tags(asList("tag1", "tag2"))
                                        .dockerfile("Dockerfile")
                                        .context("context")
                                        .target("target")
                                        .buildArgs(asList("arg1", "arg2"))
                                        .labels(Collections.singletonMap("label", "label1"))
                                        .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_REPO", "harness");
    expected.put("PLUGIN_TAGS", "tag1,tag2");
    expected.put("PLUGIN_DOCKERFILE", "Dockerfile");
    expected.put("PLUGIN_CONTEXT", "context");
    expected.put("PLUGIN_TARGET", "target");
    expected.put("PLUGIN_BUILD_ARGS", "arg1,arg2");
    expected.put("PLUGIN_CUSTOM_LABELS", "label=label1");
    Map<String, String> pluginCompatiblePublishStepEnvVariables =
        PluginSettingUtils.getPluginCompatibleEnvVariables(dockerStepInfo);
    assertThat(pluginCompatiblePublishStepEnvVariables).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetRestoreCacheS3StepInfoEnvVariables() {
    RestoreCacheS3StepInfo restoreCacheS3StepInfo =
        RestoreCacheS3StepInfo.builder().key("key").target("target").bucket("bucket").endpoint("endpoint").build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_PATH", "target");
    expected.put("PLUGIN_ROOT", "bucket");
    expected.put("PLUGIN_ENDPOINT", "endpoint");
    expected.put("PLUGIN_FILENAME", "key.tar");
    expected.put("PLUGIN_RESTORE", "true");
    Map<String, String> pluginCompatibleCacheStepEnvVariables =
        PluginSettingUtils.getPluginCompatibleEnvVariables(restoreCacheS3StepInfo);
    assertThat(pluginCompatibleCacheStepEnvVariables).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetSaveCacheS3StepInfoEnvVariables() {
    SaveCacheS3StepInfo saveCacheS3StepInfo = SaveCacheS3StepInfo.builder()
                                                  .key("key")
                                                  .target("target")
                                                  .bucket("bucket")
                                                  .sourcePath(asList("path1", "path2"))
                                                  .endpoint("endpoint")
                                                  .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_PATH", "target");
    expected.put("PLUGIN_MOUNT", "path1,path2");
    expected.put("PLUGIN_ROOT", "bucket");
    expected.put("PLUGIN_ENDPOINT", "endpoint");
    expected.put("PLUGIN_FILENAME", "key.tar");
    expected.put("PLUGIN_REBUILD", "true");
    Map<String, String> pluginCompatibleCacheStepEnvVariables =
        PluginSettingUtils.getPluginCompatibleEnvVariables(saveCacheS3StepInfo);
    assertThat(pluginCompatibleCacheStepEnvVariables).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetRestoreCacheGCSStepInfoEnvVariables() {
    RestoreCacheGCSStepInfo restoreCacheGCSStepInfo =
        RestoreCacheGCSStepInfo.builder().key("key").target("target").bucket("bucket").build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_PATH", "target");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_FILENAME", "key.tar");
    expected.put("PLUGIN_RESTORE", "true");
    Map<String, String> pluginCompatibleCacheStepEnvVariables =
        PluginSettingUtils.getPluginCompatibleEnvVariables(restoreCacheGCSStepInfo);
    assertThat(pluginCompatibleCacheStepEnvVariables).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetSaveCacheGCSStepInfoEnvVariables() {
    SaveCacheGCSStepInfo saveCacheGCSStepInfo = SaveCacheGCSStepInfo.builder()
                                                    .key("key")
                                                    .target("target")
                                                    .bucket("bucket")
                                                    .sourcePath(asList("path1", "path2"))
                                                    .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_PATH", "target");
    expected.put("PLUGIN_MOUNT", "path1,path2");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_FILENAME", "key.tar");
    expected.put("PLUGIN_REBUILD", "true");
    Map<String, String> pluginCompatibleCacheStepEnvVariables =
        PluginSettingUtils.getPluginCompatibleEnvVariables(saveCacheGCSStepInfo);
    assertThat(pluginCompatibleCacheStepEnvVariables).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetUploadToS3StepInfoEnvVariables() {
    UploadToS3StepInfo uploadToS3StepInfo = UploadToS3StepInfo.builder()
                                                .endpoint("endpoint")
                                                .region("region")
                                                .bucket("bucket")
                                                .sourcePath("sources")
                                                .target("target")
                                                .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_ENDPOINT", "endpoint");
    expected.put("PLUGIN_REGION", "region");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_SOURCE", "sources");
    expected.put("PLUGIN_TARGET", "target");

    Map<String, String> pluginCompatibleUploadStepEnvVariables =
        PluginSettingUtils.getPluginCompatibleEnvVariables(uploadToS3StepInfo);
    assertThat(pluginCompatibleUploadStepEnvVariables).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetUploadToGCSStepInfoEnvVariables() {
    UploadToGCSStepInfo uploadToS3StepInfo = UploadToGCSStepInfo.builder()
                                                 .bucket("bucket")
                                                 .sourcePath("/step-exec/workspace/pom.xml")
                                                 .target("dir/pom.xml")
                                                 .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_SOURCE", "/step-exec/workspace/pom.xml");
    expected.put("PLUGIN_TARGET", "bucket/dir/pom.xml");

    Map<String, String> pluginCompatibleUploadStepEnvVariables =
        PluginSettingUtils.getPluginCompatibleEnvVariables(uploadToS3StepInfo);
    assertThat(pluginCompatibleUploadStepEnvVariables).isEqualTo(expected);
  }
}