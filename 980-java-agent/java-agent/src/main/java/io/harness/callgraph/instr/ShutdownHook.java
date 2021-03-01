package io.harness.callgraph.instr;

import io.harness.callgraph.util.log.Logger;

public class ShutdownHook {
  private static final Logger logger = new Logger(ShutdownHook.class);
  public static void init() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        logger.info("JVM Shutdown triggered");
        try {
          CallRecorder.shutdown();
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
          // might be triggered when shutdown already ongoing
        }
      }
    });
  }
}
