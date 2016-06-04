package software.wings.dl;

import com.google.inject.Inject;

import com.mongodb.client.gridfs.GridFSBucket;
import software.wings.service.intfc.FileService.FileBucket;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public final class FileBucketHelper {
  @Inject private WingsPersistence wingsPersistence;

  /**
   * Gets the or create file bucket.
   *
   * @param fileBucket the file bucket
   * @return the or create file bucket
   */
  public GridFSBucket getOrCreateFileBucket(FileBucket fileBucket) {
    return wingsPersistence.getOrCreateGridFSBucket(fileBucket.getName());
  }
}
