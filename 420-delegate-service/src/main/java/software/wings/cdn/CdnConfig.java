package software.wings.cdn;

import io.harness.expression.ConfigSecret;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CdnConfig {
  String url;
  String keyName;
  @ConfigSecret String keySecret;
  String delegateJarPath;
  String watcherJarBasePath;
  String watcherJarPath;
  String watcherMetaDataFilePath;
  Map<String, String> cdnJreTarPaths;
  String alpnJarPath;
}
