package software.wings.beans;

import com.github.reinert.jjschema.Attributes;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Builder
@FieldNameConstants(innerTypeName = "AwsInstanceFilterKeys")
public class AwsInstanceFilter {
  @Attributes(title = "VPC") private List<String> vpcIds = new ArrayList<>();
  @Attributes(title = "Tags") private List<Tag> tags = new ArrayList<>();

  /**
   * Gets vpc ids.
   *
   * @return the vpc ids
   */
  public List<String> getVpcIds() {
    return vpcIds;
  }

  /**
   * Sets vpc ids.
   *
   * @param vpcIds the vpc ids
   */
  public void setVpcIds(List<String> vpcIds) {
    this.vpcIds = vpcIds;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  @Data
  @Builder
  public static class Tag {
    private String key;
    private String value;
  }
}
