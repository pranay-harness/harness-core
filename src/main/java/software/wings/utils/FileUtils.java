package software.wings.utils;

import java.io.File;
import java.util.Random;

public class FileUtils {
  public static File createTempDirPath() {
    String tempDirPath = System.getProperty("java.io.tmpdir");

    File f = null;
    boolean created = false;
    do {
      Random r = new Random(1000);
      f = new File(tempDirPath, System.currentTimeMillis() + "-" + r.nextInt(1000));
      if (!f.exists()) {
        created = f.mkdirs();
      }
    } while (!created);
    return f;
  }
}
