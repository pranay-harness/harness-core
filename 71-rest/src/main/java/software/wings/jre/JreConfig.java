package software.wings.jre;

import com.google.inject.Singleton;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JreConfig {
  String jreDir;
  String jreDirMacOs;
  String jreTarPathSolaris;
  String jreTarPathMacOs;
  String jreTarPathLinux;
}
