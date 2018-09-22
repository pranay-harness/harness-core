package software.wings.beans;

import com.github.reinert.jjschema.Attributes;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Builder
public class AwsInstanceFilter {
  @Attributes(title = "VPC") private List<String> vpcIds = new ArrayList<>();
  @Attributes(title = "Subnets") private List<String> subnetIds = new ArrayList<>();
  @Attributes(title = "Security Groups") private List<String> securityGroupIds = new ArrayList<>();
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

  /**
   * Gets subnet ids.
   *
   * @return the subnet ids
   */
  public List<String> getSubnetIds() {
    return subnetIds;
  }

  /**
   * Sets subnet ids.
   *
   * @param subnetIds the subnet ids
   */
  public void setSubnetIds(List<String> subnetIds) {
    this.subnetIds = subnetIds;
  }

  /**
   * Gets security group ids.
   *
   * @return the security group ids
   */
  public List<String> getSecurityGroupIds() {
    return securityGroupIds;
  }

  /**
   * Sets security group ids.
   *
   * @param securityGroupIds the security group ids
   */
  public void setSecurityGroupIds(List<String> securityGroupIds) {
    this.securityGroupIds = securityGroupIds;
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
