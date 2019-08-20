package software.wings.beans.trigger;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitHubPayloadSource.class, name = "GITHUB")
  , @JsonSubTypes.Type(value = GitLabsPayloadSource.class, name = "GITLABS"),
      @JsonSubTypes.Type(value = BitBucketPayloadSource.class, name = "BITBUCKET")
})
public interface PayloadSource {
  enum Type { BITBUCKET, GITHUB, GITLABS }
  Type getType();
}
