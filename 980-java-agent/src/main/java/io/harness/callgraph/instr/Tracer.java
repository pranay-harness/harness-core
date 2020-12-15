package io.harness.callgraph.instr;

import io.harness.callgraph.util.config.Config;
import io.harness.callgraph.util.config.ConfigReader;
import io.harness.callgraph.util.log.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

/**
 * Instrument the target classes.
 */
public class Tracer {
  /**
   * Program entry point. Loads the config and starts itself as instrumentation.
   *
   * @param argument        command line argument. Should specify a config file
   * @param instrumentation instrumentation
   * @throws IOException            io error
   * @throws IllegalAccessException problem loading the config options
   */
  public static void premain(String argument, Instrumentation instrumentation)
      throws IOException, IllegalAccessException {
    if (argument != null) {
      new ConfigReader(Tracer.class.getResourceAsStream("/io/harness/callgraph/defaults.ini"),
          new FileInputStream(new File(argument)))
          .read();
    } else {
      System.err.println("Config file was not specified, using the default config options instead.");
      new ConfigReader(Tracer.class.getResourceAsStream("/io/harness/callgraph/defaults.ini")).read();
    }

    Logger.init();

    ShutdownHook.init();

    List<String> includes = Arrays.asList(Config.getInst().instrPackages());
    new ByteBuddyInstr(includes).instrument(instrumentation);
  }
}
