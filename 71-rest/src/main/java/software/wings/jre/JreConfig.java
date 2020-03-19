package software.wings.jre;

import com.google.inject.Singleton;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Singleton
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JreConfig {
  String version;
  String jreDirectory;
  String jreMacDirectory;
  String jreTarPath;
}
