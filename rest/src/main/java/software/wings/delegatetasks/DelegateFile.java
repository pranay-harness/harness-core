package software.wings.delegatetasks;

import software.wings.beans.ChecksumType;
import software.wings.beans.FileMetadata;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by rishi on 12/19/16.
 */
public class DelegateFile extends FileMetadata {
  private String fileId;
  private FileService.FileBucket bucket;
  private String entityId;
  private String localFilePath;

  private String delegateId;
  private String taskId;
  private String accountId;
  private String appId;

  public String getFileId() {
    return fileId;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  public FileBucket getBucket() {
    return bucket;
  }

  public void setBucket(FileBucket bucket) {
    this.bucket = bucket;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getLocalFilePath() {
    return localFilePath;
  }

  public void setLocalFilePath(String localFilePath) {
    this.localFilePath = localFilePath;
  }

  public String getDelegateId() {
    return delegateId;
  }

  public void setDelegateId(String delegateId) {
    this.delegateId = delegateId;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public InputStream getContentSource() throws FileNotFoundException {
    if (localFilePath == null) {
      return null;
    }
    File file = new File(localFilePath);
    if (!file.exists()) {
      return null;
    }
    return new FileInputStream(file);
  }

  public static final class DelegateFileBuilder {
    private String fileName;
    private String mimeType;
    private ChecksumType checksumType;
    private String checksum;
    private String relativePath;
    private String fileId;
    private FileBucket bucket;
    private String entityId;
    private String localFilePath;
    private String delegateId;
    private String taskId;
    private String accountId;
    private String appId;

    private DelegateFileBuilder() {}

    public static DelegateFileBuilder aDelegateFile() {
      return new DelegateFileBuilder();
    }

    public DelegateFileBuilder withFileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public DelegateFileBuilder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    public DelegateFileBuilder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    public DelegateFileBuilder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    public DelegateFileBuilder withRelativePath(String relativePath) {
      this.relativePath = relativePath;
      return this;
    }

    public DelegateFileBuilder withFileId(String fileId) {
      this.fileId = fileId;
      return this;
    }

    public DelegateFileBuilder withBucket(FileBucket bucket) {
      this.bucket = bucket;
      return this;
    }

    public DelegateFileBuilder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public DelegateFileBuilder withLocalFilePath(String localFilePath) {
      this.localFilePath = localFilePath;
      return this;
    }

    public DelegateFileBuilder withDelegateId(String delegateId) {
      this.delegateId = delegateId;
      return this;
    }

    public DelegateFileBuilder withTaskId(String taskId) {
      this.taskId = taskId;
      return this;
    }

    public DelegateFileBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public DelegateFileBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public DelegateFile build() {
      DelegateFile delegateFile = new DelegateFile();
      delegateFile.setFileName(fileName);
      delegateFile.setMimeType(mimeType);
      delegateFile.setChecksumType(checksumType);
      delegateFile.setChecksum(checksum);
      delegateFile.setRelativePath(relativePath);
      delegateFile.setFileId(fileId);
      delegateFile.setBucket(bucket);
      delegateFile.setEntityId(entityId);
      delegateFile.setLocalFilePath(localFilePath);
      delegateFile.setDelegateId(delegateId);
      delegateFile.setTaskId(taskId);
      delegateFile.setAccountId(accountId);
      delegateFile.appId = this.appId;
      return delegateFile;
    }
  }
}
