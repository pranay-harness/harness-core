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
import io.harness.beans.steps.stepinfo.UploadToArtifactoryStepInfo;
import io.harness.beans.steps.stepinfo.UploadToGCSStepInfo;
import io.harness.beans.steps.stepinfo.UploadToS3StepInfo;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionTest;
import io.harness.pms.yaml.ParameterField;
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
  public void shouldGetUploadToArtifactoryStepInfoStepEnvVariables() {
    UploadToArtifactoryStepInfo uploadToArtifactoryStepInfo =
        UploadToArtifactoryStepInfo.builder()
            .target(ParameterField.createValueField("repo/wings/software/module/1.0.0-SNAPSHOT"))
            .sourcePath(ParameterField.createValueField("target/libmodule.jar"))
            .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_TARGET", "repo/wings/software/module/1.0.0-SNAPSHOT");
    expected.put("PLUGIN_SOURCE", "target/libmodule.jar");

    Map<String, String> actual =
        PluginSettingUtils.getPluginCompatibleEnvVariables(uploadToArtifactoryStepInfo, "identifier");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetGCRStepInfoEnvVariables() {
    GCRStepInfo gcrStepInfo =
        GCRStepInfo.builder()
            .host(ParameterField.createValueField("gcr.io/"))
            .projectID(ParameterField.createValueField("/ci"))
            .imageName(ParameterField.createValueField("harness"))
            .tags(ParameterField.createValueField(asList("tag1", "tag2")))
            .dockerfile(ParameterField.createValueField("Dockerfile"))
            .context(ParameterField.createValueField("context"))
            .target(ParameterField.createValueField("target"))
            .buildArgs(ParameterField.createValueField(Collections.singletonMap("arg1", "value1")))
            .labels(ParameterField.createValueField(Collections.singletonMap("label", "label1")))
            .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_REGISTRY", "gcr.io/ci");
    expected.put("PLUGIN_REPO", "harness");
    expected.put("PLUGIN_TAGS", "tag1,tag2");
    expected.put("PLUGIN_DOCKERFILE", "Dockerfile");
    expected.put("PLUGIN_CONTEXT", "context");
    expected.put("PLUGIN_TARGET", "target");
    expected.put("PLUGIN_BUILD_ARGS", "arg1=value1");
    expected.put("PLUGIN_CUSTOM_LABELS", "label=label1");
    Map<String, String> actual = PluginSettingUtils.getPluginCompatibleEnvVariables(gcrStepInfo, "identifier");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetECRStepInfoStepEnvVariables() {
    ECRStepInfo ecrStepInfo =
        ECRStepInfo.builder()
            .account(ParameterField.createValueField("6874654867"))
            .region(ParameterField.createValueField("eu-central-1"))
            .imageName(ParameterField.createValueField("harness"))
            .tags(ParameterField.createValueField(asList("tag1", "tag2")))
            .dockerfile(ParameterField.createValueField("Dockerfile"))
            .context(ParameterField.createValueField("context"))
            .target(ParameterField.createValueField("target"))
            .buildArgs(ParameterField.createValueField(Collections.singletonMap("arg1", "value1")))
            .labels(ParameterField.createValueField(Collections.singletonMap("label", "label1")))
            .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_REGISTRY", "6874654867.dkr.ecr.eu-central-1.amazonaws.com");
    expected.put("PLUGIN_REPO", "harness");
    expected.put("PLUGIN_TAGS", "tag1,tag2");
    expected.put("PLUGIN_DOCKERFILE", "Dockerfile");
    expected.put("PLUGIN_CONTEXT", "context");
    expected.put("PLUGIN_TARGET", "target");
    expected.put("PLUGIN_BUILD_ARGS", "arg1=value1");
    expected.put("PLUGIN_CUSTOM_LABELS", "label=label1");
    Map<String, String> actual = PluginSettingUtils.getPluginCompatibleEnvVariables(ecrStepInfo, "identifier");
    assertThat(actual).isEqualTo(expected);
  }
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetDockerStepInfoEnvVariables() {
    DockerStepInfo dockerStepInfo =
        DockerStepInfo.builder()
            .repo(ParameterField.createValueField("harness"))
            .tags(ParameterField.createValueField(asList("tag1", "tag2")))
            .dockerfile(ParameterField.createValueField("Dockerfile"))
            .context(ParameterField.createValueField("context"))
            .target(ParameterField.createValueField("target"))
            .buildArgs(ParameterField.createValueField(Collections.singletonMap("arg1", "value1")))
            .labels(ParameterField.createValueField(Collections.singletonMap("label", "label1")))
            .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_REPO", "harness");
    expected.put("PLUGIN_TAGS", "tag1,tag2");
    expected.put("PLUGIN_DOCKERFILE", "Dockerfile");
    expected.put("PLUGIN_CONTEXT", "context");
    expected.put("PLUGIN_TARGET", "target");
    expected.put("PLUGIN_BUILD_ARGS", "arg1=value1");
    expected.put("PLUGIN_CUSTOM_LABELS", "label=label1");
    Map<String, String> actual = PluginSettingUtils.getPluginCompatibleEnvVariables(dockerStepInfo, "identifier");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetRestoreCacheS3StepInfoEnvVariables() {
    RestoreCacheS3StepInfo restoreCacheS3StepInfo = RestoreCacheS3StepInfo.builder()
                                                        .key(ParameterField.createValueField("key"))
                                                        .target(ParameterField.createValueField("target"))
                                                        .bucket(ParameterField.createValueField("bucket"))
                                                        .endpoint(ParameterField.createValueField("endpoint"))
                                                        .region(ParameterField.createValueField("region"))
                                                        .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_PATH", "target");
    expected.put("PLUGIN_ROOT", "bucket");
    expected.put("PLUGIN_ENDPOINT", "endpoint");
    expected.put("PLUGIN_FILENAME", "key.tar");
    expected.put("PLUGIN_RESTORE", "true");
    expected.put("PLUGIN_REGION", "region");
    Map<String, String> actual =
        PluginSettingUtils.getPluginCompatibleEnvVariables(restoreCacheS3StepInfo, "identifier");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetSaveCacheS3StepInfoEnvVariables() {
    SaveCacheS3StepInfo saveCacheS3StepInfo =
        SaveCacheS3StepInfo.builder()
            .key(ParameterField.createValueField("key"))
            .target(ParameterField.createValueField("target"))
            .bucket(ParameterField.createValueField("bucket"))
            .region(ParameterField.createValueField("region"))
            .sourcePaths(ParameterField.createValueField(asList("path1", "path2")))
            .endpoint(ParameterField.createValueField("endpoint"))
            .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_PATH", "target");
    expected.put("PLUGIN_MOUNT", "path1,path2");
    expected.put("PLUGIN_ROOT", "bucket");
    expected.put("PLUGIN_ENDPOINT", "endpoint");
    expected.put("PLUGIN_FILENAME", "key.tar");
    expected.put("PLUGIN_REBUILD", "true");
    expected.put("PLUGIN_REGION", "region");
    Map<String, String> actual = PluginSettingUtils.getPluginCompatibleEnvVariables(saveCacheS3StepInfo, "identifier");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetRestoreCacheGCSStepInfoEnvVariables() {
    RestoreCacheGCSStepInfo restoreCacheGCSStepInfo = RestoreCacheGCSStepInfo.builder()
                                                          .key(ParameterField.createValueField("key"))
                                                          .target(ParameterField.createValueField("target"))
                                                          .bucket(ParameterField.createValueField("bucket"))
                                                          .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_PATH", "target");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_FILENAME", "key.tar");
    expected.put("PLUGIN_RESTORE", "true");
    Map<String, String> actual =
        PluginSettingUtils.getPluginCompatibleEnvVariables(restoreCacheGCSStepInfo, "identifier");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetSaveCacheGCSStepInfoEnvVariables() {
    SaveCacheGCSStepInfo saveCacheGCSStepInfo =
        SaveCacheGCSStepInfo.builder()
            .key(ParameterField.createValueField("key"))
            .target(ParameterField.createValueField("target"))
            .bucket(ParameterField.createValueField("bucket"))
            .sourcePaths(ParameterField.createValueField(asList("path1", "path2")))
            .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_PATH", "target");
    expected.put("PLUGIN_MOUNT", "path1,path2");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_FILENAME", "key.tar");
    expected.put("PLUGIN_REBUILD", "true");
    Map<String, String> actual = PluginSettingUtils.getPluginCompatibleEnvVariables(saveCacheGCSStepInfo, "identifier");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetUploadToS3StepInfoEnvVariables() {
    UploadToS3StepInfo uploadToS3StepInfo = UploadToS3StepInfo.builder()
                                                .endpoint(ParameterField.createValueField("endpoint"))
                                                .region(ParameterField.createValueField("region"))
                                                .bucket(ParameterField.createValueField("bucket"))
                                                .sourcePath(ParameterField.createValueField("sources"))
                                                .target(ParameterField.createValueField("target"))
                                                .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_ENDPOINT", "endpoint");
    expected.put("PLUGIN_REGION", "region");
    expected.put("PLUGIN_BUCKET", "bucket");
    expected.put("PLUGIN_SOURCE", "sources");
    expected.put("PLUGIN_TARGET", "target");

    Map<String, String> actual = PluginSettingUtils.getPluginCompatibleEnvVariables(uploadToS3StepInfo, "identifier");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetUploadToGCSStepInfoEnvVariables() {
    UploadToGCSStepInfo uploadToS3StepInfo =
        UploadToGCSStepInfo.builder()
            .bucket(ParameterField.createValueField("bucket"))
            .sourcePath(ParameterField.createValueField("/step-exec/workspace/pom.xml"))
            .target(ParameterField.createValueField("dir/pom.xml"))
            .build();
    Map<String, String> expected = new HashMap<>();
    expected.put("PLUGIN_SOURCE", "/step-exec/workspace/pom.xml");
    expected.put("PLUGIN_TARGET", "bucket/dir/pom.xml");

    Map<String, String> actual = PluginSettingUtils.getPluginCompatibleEnvVariables(uploadToS3StepInfo, "identifier");
    assertThat(actual).isEqualTo(expected);
  }
}