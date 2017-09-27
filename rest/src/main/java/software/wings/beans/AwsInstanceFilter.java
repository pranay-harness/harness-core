package software.wings.beans;

import com.github.reinert.jjschema.Attributes;

import java.util.ArrayList;
import java.util.List;

public class AwsInstanceFilter {
  @Attributes(title = "VPC") private List<String> vpcIds;
  @Attributes(title = "Subnets") private List<String> subnetIds = new ArrayList<>();
  @Attributes(title = "Security Groups") private List<String> securityGroupIds = new ArrayList<>();

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
}
