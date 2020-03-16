package software.wings.cdn;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.wings.jre.JreConfig;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CdnConfig {
  String url;
  String keyName;
  String keySecret;
  String delegateJarPath;
  String watcherJarPath;
  String watcherMetaDataFilePath;
  JreConfig jreConfig;
}
