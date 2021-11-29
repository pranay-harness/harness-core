package io.harness;

import static io.harness.reflection.HarnessReflections.CLASSPATH_METADATA_FILE_NAME;

import io.harness.packages.HarnessPackages;

import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.serializers.JsonSerializer;
import org.reflections.util.ConfigurationBuilder;

@Slf4j
public class ScanClasspathMetadataCommand extends Command {
  public ScanClasspathMetadataCommand() {
    super("scan-classpath-metadata", "");
  }

  @Override
  public void configure(Subparser subparser) {}

  @Override
  public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
    String savePath = Paths.get(System.getProperty("user.dir"), CLASSPATH_METADATA_FILE_NAME).toString();
    Configuration configuration =
        new ConfigurationBuilder()
            .forPackages(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS)
            .addScanners(new ResourcesScanner());
    new Reflections(configuration).save(savePath, new JsonSerializer());
  }
}
