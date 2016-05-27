package software.wings.dl;

import com.google.inject.Inject;

import com.mongodb.client.gridfs.GridFSBucket;
import software.wings.service.intfc.FileService.FileBucket;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public final class FileBucketHelper {
  @Inject private WingsPersistence wingsPersistence;

  public GridFSBucket getOrCreateFileBucket(FileBucket fileBucket) {
    return wingsPersistence.getOrCreateGridFSBucket(fileBucket.getName());
  }
}
