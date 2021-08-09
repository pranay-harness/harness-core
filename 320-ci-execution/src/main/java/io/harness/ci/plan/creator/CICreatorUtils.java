package io.harness.ci.plan.creator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.Sets;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
public class CICreatorUtils {
  public Set<String> getSupportedSteps() {
    return Sets.newHashSet("SaveCacheS3", "Test", "RunTests", "SaveCache", "liteEngineTask", "GitClone",
        "BuildAndPushGCR", "BuildAndPushECR", "BuildAndPushDockerRegistry", "Cleanup", "Plugin", "PublishArtifacts",
        "RestoreCacheGCS", "RestoreCacheS3", "RestoreCache", "SaveCacheGCS", "Run", "S3Upload", "GCSUpload",
        "ArtifactoryUpload");
  }
}
