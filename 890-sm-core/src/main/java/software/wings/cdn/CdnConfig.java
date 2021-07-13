package software.wings.cdn;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PL)
public class CdnConfig {
  String url;
  String keyName;
  String keySecret;
  String delegateJarPath;
  String watcherJarBasePath;
  String watcherJarPath;
  String watcherMetaDataFilePath;
  Map<String, String> cdnJreTarPaths;
  String alpnJarPath;
}
