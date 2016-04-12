package software.wings.service.intfc;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import software.wings.beans.FileMetadata;
import software.wings.dl.WingsPersistence;

import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public interface FileService {
  public String saveFile(FileMetadata fileMetadata, InputStream in, FileBucket fileBucket);

  public File download(String fileId, File file, FileBucket fileBucket);

  public void downloadToStream(String fileId, OutputStream op, FileBucket fileBucket);

  public GridFSFile getGridFsFile(String fileId, FileBucket fileBucket);

  public String uploadFromStream(String filename, InputStream in, FileBucket fileBucket, GridFSUploadOptions options);

  public static enum FileBucket {
    LOB("lob"),
    AUDITS("audits"),
    CONFIGS("configs"),
    LOGS("logs");

    FileBucket(String name, long chunkSize) {
      this.name = name;
      this.chunkSize = chunkSize;
      this.gridFSBucket = wingsPersistence.createGridFSBucket(name);
    }

    FileBucket(String name) {
      this(name, 16 * 1024 * 1024);
    }

    @Inject private WingsPersistence wingsPersistence;
    private String name;
    private long chunkSize;
    private GridFSBucket gridFSBucket;

    public String getName() {
      return name;
    }

    public long getChunkSize() {
      return chunkSize;
    }

    public GridFSBucket getGridFSBucket() {
      return gridFSBucket;
    }
  }
}
