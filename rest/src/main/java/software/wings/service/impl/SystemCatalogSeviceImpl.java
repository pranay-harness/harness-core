package software.wings.service.impl;

import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import software.wings.app.FileUploadLimit;
import software.wings.beans.AppContainer;
import software.wings.beans.Base;
import software.wings.beans.SystemCatalog;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.SystemCatalogService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.FileType;
import software.wings.utils.FileTypeDetector;
import software.wings.utils.Misc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by sgurubelli on 5/23/17.
 */
@ValidateOnExecution
@Singleton
public class SystemCatalogSeviceImpl implements SystemCatalogService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
  /**
   * {@inheritDoc}
   */
  @Override
  public SystemCatalog save(SystemCatalog systemCatalog, String url, FileBucket fileBucket, long size) {
    uploadSystemCatalogFile(systemCatalog, url, fileBucket, size);
    return wingsPersistence.saveAndGet(SystemCatalog.class, systemCatalog);
  }

  private void uploadSystemCatalogFile(SystemCatalog systemCatalog, String url, FileBucket fileBucket, long size) {
    BufferedInputStream in = new BufferedInputStream(BoundedInputStream.getBoundedStreamForUrl(url, size));

    String fileId = fileService.saveFile(systemCatalog, in, fileBucket);
    systemCatalog.setFileUuid(fileId);
    systemCatalog.setAppId(Base.GLOBAL_APP_ID);

    File tempFile = new File(
        System.getProperty("java.io.tmpdir"), systemCatalog.getCatalogType().name() + Thread.currentThread().getId());
    fileService.download(fileId, tempFile, fileBucket);

    Misc.ignoreException(() -> {
      BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(tempFile));
      FileType fileType = FileTypeDetector.detectType(bufferedInputStream);
      systemCatalog.setFileType(fileType);
      systemCatalog.setStackRootDirectory(fileType.getRoot(bufferedInputStream));
    });

    tempFile.delete();
  }

  @Override
  public List<SystemCatalog> list(PageRequest<SystemCatalog> pageRequest) {
    return wingsPersistence.query(SystemCatalog.class, pageRequest).getResponse();
  }
}
