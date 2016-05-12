package software.wings.integration;

import static java.lang.Integer.MAX_VALUE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * Created by anubhaw on 5/11/16.
 */

public class IntegrationTestUtil {
  private static Random random = new Random();

  public static File createHostsFile(File file, int numHosts) throws IOException {
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write("HOST,OS,ACCESS_TYPE\n"); // Header
    for (int idx = 1; idx <= numHosts; idx++) {
      out.write(String.format("host%s.app.com,Linux-RHL,SSH_SUDO_APP_ACCOUNT\n", idx));
    }
    out.close();
    return file;
  }

  public static int randomInt(int low, int high) {
    return random.nextInt(high - low) + low;
  }

  public static int randomInt(int high) {
    return randomInt(0, high);
  }

  public static int randomInt() {
    return randomInt(0, MAX_VALUE);
  }
}
