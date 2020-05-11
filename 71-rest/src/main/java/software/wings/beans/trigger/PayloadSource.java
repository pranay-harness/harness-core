package software.wings.beans.trigger;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitHubPayloadSource.class, name = "GITHUB")
  , @JsonSubTypes.Type(value = GitLabsPayloadSource.class, name = "GITLAB"),
      @JsonSubTypes.Type(value = CustomPayloadSource.class, name = "CUSTOM"),
      @JsonSubTypes.Type(value = BitBucketPayloadSource.class, name = "BITBUCKET")
})
public interface PayloadSource {
  enum Type { BITBUCKET, GITHUB, GITLAB, CUSTOM }
  Type getType();
}
