package software.wings.delegatetasks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class DelegateFile extends FileMetadata {
  private String fileId;
  private FileService.FileBucket bucket;
  private String entityId;
  private String localFilePath;

  private String delegateId;
  private String taskId;
  private String accountId;
  private String appId;
  private long length;
  private InputStream inputStream;

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

  /**
   * Getter for property 'length'.
   *
   * @return Value for property 'length'.
   */
  public long getLength() {
    return length;
  }

  /**
   * Setter for property 'length'.
   *
   * @param length Value to set for property 'length'.
   */
  public void setLength(long length) {
    this.length = length;
  }

  public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  @JsonIgnore
  public InputStream getContentSource() throws FileNotFoundException {
    if (inputStream != null) {
      return inputStream;
    }

    if (localFilePath == null) {
      return null;
    }
    File file = new File(localFilePath);
    if (!file.exists()) {
      return null;
    }
    return new FileInputStream(file);
  }

  public static final class Builder {
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
    private long length;
    private InputStream inputStream;

    private Builder() {}

    public static Builder aDelegateFile() {
      return new Builder();
    }

    public Builder withFileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public Builder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    public Builder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    public Builder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    public Builder withRelativePath(String relativePath) {
      this.relativePath = relativePath;
      return this;
    }

    public Builder withFileId(String fileId) {
      this.fileId = fileId;
      return this;
    }

    public Builder withBucket(FileBucket bucket) {
      this.bucket = bucket;
      return this;
    }

    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public Builder withLocalFilePath(String localFilePath) {
      this.localFilePath = localFilePath;
      return this;
    }

    public Builder withDelegateId(String delegateId) {
      this.delegateId = delegateId;
      return this;
    }

    public Builder withTaskId(String taskId) {
      this.taskId = taskId;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withLength(long length) {
      this.length = length;
      return this;
    }

    public Builder withInputStream(InputStream inputStream) {
      this.inputStream = inputStream;
      return this;
    }

    public Builder but() {
      return aDelegateFile()
          .withFileName(fileName)
          .withMimeType(mimeType)
          .withChecksumType(checksumType)
          .withChecksum(checksum)
          .withRelativePath(relativePath)
          .withFileId(fileId)
          .withBucket(bucket)
          .withEntityId(entityId)
          .withLocalFilePath(localFilePath)
          .withDelegateId(delegateId)
          .withTaskId(taskId)
          .withAccountId(accountId)
          .withAppId(appId)
          .withLength(length)
          .withInputStream(inputStream);
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
      delegateFile.setAppId(appId);
      delegateFile.setLength(length);
      delegateFile.setInputStream(inputStream);
      return delegateFile;
    }
  }
}
