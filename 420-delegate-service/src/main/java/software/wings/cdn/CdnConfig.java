package software.wings.cdn;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
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
