package software.wings.utils;

import java.io.File;
import java.util.Random;

// TODO: Auto-generated Javadoc

/**
 * The Class FileUtils.
 */
public class FileUtils {
  /**
   * Creates the temp dir path.
   *
   * @return the file
   */
  public static File createTempDirPath() {
    String tempDirPath = System.getProperty("java.io.tmpdir");

    File file = null;
    boolean created = false;
    do {
      Random random = new Random(1000);
      file = new File(tempDirPath, System.currentTimeMillis() + "-" + random.nextInt(1000));
      if (!file.exists()) {
        created = file.mkdirs();
      }
    } while (!created);
    return file;
  }
}
