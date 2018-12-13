package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.service.intfc.FileService.FileBucket;

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
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(value = "gcsFileMetadata", noClassnameStored = true)
public class GcsFileMetadata extends Base {
  @NotEmpty @Indexed private String accountId;
  @NotEmpty @Indexed private String fileId; // Mongo GridFs fileId.
  @NotEmpty @Indexed private String gcsFileId;
  @NotEmpty @Indexed private String fileName;
  @NotEmpty @Indexed private FileBucket fileBucket;
  @Indexed private String entityId;
  @Indexed private int version;
}
