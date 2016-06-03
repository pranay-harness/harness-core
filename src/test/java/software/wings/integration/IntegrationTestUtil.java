package software.wings.integration;

import static java.lang.Integer.MAX_VALUE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/11/16.
 */

public class IntegrationTestUtil {
  private static Random random = new Random();

  /**
   * Creates the hosts file.
   *
   * @param file     the file
   * @param numHosts the num hosts
   * @return the file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File createHostsFile(File file, int numHosts) throws IOException {
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write("HOST\n"); // Header
    for (int idx = 1; idx <= numHosts; idx++) {
      out.write(String.format("host%s.app.com\n", idx));
    }
    out.close();
    return file;
  }

  /**
   * Random int.
   *
   * @param low  the low
   * @param high the high
   * @return the int
   */
  public static int randomInt(int low, int high) {
    return random.nextInt(high - low) + low;
  }

  /**
   * Random int.
   *
   * @param high the high
   * @return the int
   */
  public static int randomInt(int high) {
    return randomInt(0, high);
  }

  /**
   * Random int.
   *
   * @return the int
   */
  public static int randomInt() {
    return randomInt(0, MAX_VALUE);
  }
}
