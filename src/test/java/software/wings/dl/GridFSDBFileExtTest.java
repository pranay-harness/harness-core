package software.wings.dl;

import com.mongodb.MongoClient;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by anubhaw on 3/4/16.
 */
public class GridFSDBFileExtTest {
  GridFSDBFileExt gridFSDBFileExt = new GridFSDBFileExt(new MongoClient("localhost").getDatabase("test"), "test", 6);

  @Test
  public void testAppendToFile() throws Exception {
    gridFSDBFileExt.appendToFile("56df29c11e6c0305b8c7a84c", "FIRST LOG LINE\n");
    gridFSDBFileExt.appendToFile("56df29c11e6c0305b8c7a84c", "[APPENDED LOG LINE]\n");
  }

  @Test
  public void testGet() throws FileNotFoundException {
    File file = new File("/tmp/output");
    gridFSDBFileExt.downloadToStream("56df29c11e6c0305b8c7a84c", new FileOutputStream(file));
  }
}