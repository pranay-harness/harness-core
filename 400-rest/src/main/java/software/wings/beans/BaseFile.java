/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ChecksumType;
import io.harness.file.HarnessFile;
import io.harness.validation.Create;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by anubhaw on 4/13/16.
 */
@OwnedBy(PL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BaseFile extends Base implements HarnessFile {
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
