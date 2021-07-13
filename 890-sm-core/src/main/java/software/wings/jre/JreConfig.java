package software.wings.jre;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Singleton
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@OwnedBy(PL)
public class JreConfig {
  String version;
  String jreDirectory;
  String jreMacDirectory;
  String jreTarPath;
  String alpnJarPath;
}
