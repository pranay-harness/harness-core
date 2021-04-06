package harness.callgraph.instr;

import harness.callgraph.boot.TraceDispatcher;
import harness.callgraph.instr.tracer.CallableTracer;
import harness.callgraph.util.config.Config;
import harness.callgraph.util.config.ConfigReader;
import harness.callgraph.util.log.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
      new ConfigReader(
          Tracer.class.getResourceAsStream("/harness/callgraph/defaults.ini"), new FileInputStream(new File(argument)))
          .read();
    } else {
      System.err.println("Config file was not specified, using the default config options instead.");
      new ConfigReader(Tracer.class.getResourceAsStream("/harness/callgraph/defaults.ini")).read();
    }

    Logger.init();

    ShutdownHook.init();

    Set<String> includes = new HashSet<>(Arrays.asList(Config.getInst().instrPackages()));

    // Read user provided test annotations in addition to default test annotations
    Set<String> defaultTestAnnotations =
        new HashSet<>(Arrays.asList("org.junit.Test", "org.junit.jupiter.api.Test", "org.testng.annotations.Test"));

    Set<String> testAnnotations = new HashSet<>(Arrays.asList(Config.getInst().testAnnotations()));
    testAnnotations.addAll(defaultTestAnnotations);

    System.setProperty("net.bytebuddy.raw", "true"); // don't resolve generics as it causes JVM crashes in some cases in
                                                     // jdk1.8. for later versions beyond 1.8, we can enable

    TraceDispatcher.INSTANCE = new CallableTracer();
    new ByteBuddyInstr(includes, testAnnotations).instrument(instrumentation);
  }
}
