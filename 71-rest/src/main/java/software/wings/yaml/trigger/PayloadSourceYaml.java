package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYamlWithType;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
@JsonSubTypes({
  @Type(value = BitBucketPayloadSourceYaml.class, name = "BITBUCKET")
  , @Type(value = GitlabPayloadSourceYaml.class, name = "GITLAB"),
      @Type(value = GithubPayloadSourceYaml.class, name = "GITHUB"),
})
public class PayloadSourceYaml extends BaseYamlWithType {
  public PayloadSourceYaml(String type) {
    super(type);
  }
}
