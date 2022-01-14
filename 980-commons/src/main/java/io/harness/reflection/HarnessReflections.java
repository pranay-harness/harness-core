package io.harness.reflection;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.packages.HarnessPackages;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import lombok.experimental.UtilityClass;
import org.reflections.Reflections;
import org.reflections.serializers.JsonSerializer;
import org.reflections.util.ConfigurationBuilder;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class HarnessReflections {
  public static final String CLASSPATH_METADATA_FILE_NAME = "classpath_metadata.json";
  private static final File CLASSPATH_METADATA_FILE = new File(CLASSPATH_METADATA_FILE_NAME);
  private static final AtomicReference<Reflections> reflections = new AtomicReference<>();

  public Reflections get() {
    if (reflections.get() == null) {
      if (doHarnessReflectionsFileExist()) {
        ConfigurationBuilder config = new ConfigurationBuilder();
        config.setSerializer(new JsonSerializer());

        reflections.set(new Reflections(config).collect(CLASSPATH_METADATA_FILE));
      } else {
        reflections.set(new Reflections(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS));
      }
    }

    return reflections.get();
  }

  private static boolean doHarnessReflectionsFileExist() {
    return Files.exists(CLASSPATH_METADATA_FILE.toPath());
  }
}
