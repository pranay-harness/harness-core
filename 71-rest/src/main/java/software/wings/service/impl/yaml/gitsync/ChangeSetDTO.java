package software.wings.service.impl.yaml.gitsync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.GitDetail;
import software.wings.yaml.gitSync.YamlChangeSet;

@Value
@Builder
@FieldNameConstants(innerTypeName = "OngoingCommitsDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeSetDTO {
  GitDetail gitDetail;
  YamlChangeSet.Status status;
  boolean gitToHarness;
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "status", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
  ChangesetInformation changesetInformation;
}
