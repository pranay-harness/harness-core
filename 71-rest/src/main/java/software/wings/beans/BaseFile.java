package software.wings.beans;

import com.google.common.base.MoreObjects;

import io.harness.validation.Create;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Objects;

/**
 * Created by anubhaw on 4/13/16.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BaseFile extends Base {
  @FormDataParam("name") private String name;
  private String fileUuid;
  @NotEmpty(groups = Create.class) private String fileName;
  private String mimeType;
  private long size;
  private ChecksumType checksumType = ChecksumType.MD5;
  @FormDataParam("md5") private String checksum;
  @NotEmpty protected String accountId;

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(fileUuid, name, fileName, mimeType, size, checksumType, checksum, accountId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final BaseFile other = (BaseFile) obj;
    return Objects.equals(this.fileUuid, other.fileUuid) && Objects.equals(this.name, other.name)
        && Objects.equals(this.fileName, other.fileName) && Objects.equals(this.mimeType, other.mimeType)
        && Objects.equals(this.size, other.size) && this.checksumType == other.checksumType
        && Objects.equals(this.checksum, other.checksum) && Objects.equals(this.accountId, other.accountId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fileUuid", fileUuid)
        .add("name", name)
        .add("fileName", fileName)
        .add("mimeType", mimeType)
        .add("size", size)
        .add("checksumType", checksumType)
        .add("checksum", checksum)
        .add("accountId", accountId)
        .toString();
  }
}
