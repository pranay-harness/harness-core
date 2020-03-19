package software.wings.cdn;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CdnConfig {
  String url;
  String keyName;
  String keySecret;
  String delegateJarPath;
  String watcherJarPath;
  String watcherMetaDataFilePath;
  Map<String, String> cdnJreTarPaths;
}
