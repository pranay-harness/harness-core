package software.wings.cdn;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
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
