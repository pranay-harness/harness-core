package software.wings.app;

import com.hazelcast.config.AwsConfig;
import com.hazelcast.config.TcpIpConfig;

/**
 * Created by peeyushaggarwal on 5/5/17.
 */
public class HazelcastConfiguration {
  private AwsConfig awsConfig;
  private TcpIpConfig tcpIpConfig;

  /**
   * Getter for property 'awsConfig'.
   *
   * @return Value for property 'awsConfig'.
   */
  public AwsConfig getAwsConfig() {
    return awsConfig;
  }

  /**
   * Setter for property 'awsConfig'.
   *
   * @param awsConfig Value to set for property 'awsConfig'.
   */
  public void setAwsConfig(AwsConfig awsConfig) {
    this.awsConfig = awsConfig;
  }

  /**
   * Getter for property 'tcpIpConfig'.
   *
   * @return Value for property 'tcpIpConfig'.
   */
  public TcpIpConfig getTcpIpConfig() {
    return tcpIpConfig;
  }

  /**
   * Setter for property 'tcpIpConfig'.
   *
   * @param tcpIpConfig Value to set for property 'tcpIpConfig'.
   */
  public void setTcpIpConfig(TcpIpConfig tcpIpConfig) {
    this.tcpIpConfig = tcpIpConfig;
  }
}
