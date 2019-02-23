package software.wings.beans;

import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.Map;

/**
 * Since artifacts etc. will be initially dual-written into Mongo GridFs and Google Cloud Storage, and each file id has
 * different format, we will need to use this mapping to store the file id mappings. So that we could use each format of
 * file id to look up stored files from either storage.
 *
 * Also GCS is not good at searching entries based on extra metadata such as 'entityId' and 'version', we have to save
 * this data in this Mongo collection in an indexed manner to respond to queries based on 'entityId' and 'version'
 * filter.
 *
 * @author marklu on 2018-12-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "gcsFileMetadata", noClassnameStored = true)
@HarnessExportableEntity
public class GcsFileMetadata extends Base {
  @NotEmpty @NaturalKey private String accountId;
  @NotEmpty @Indexed @NaturalKey private String fileId; // Mongo GridFs fileId.
  @NotEmpty @Indexed @NaturalKey private String gcsFileId;
  @NotEmpty @NaturalKey private String fileName;
  @NotEmpty @NaturalKey private FileBucket fileBucket;
  @NaturalKey private String entityId;
  @NaturalKey private int version;
  private long fileLength;
  private String mimeType;
  private ChecksumType checksumType;
  private String checksum;
  private Map<String, Object> others; // Additional metadata, typically used by TerraformState.
}
