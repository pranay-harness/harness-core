package io.harness.event.handler.segment;

import com.google.inject.Singleton;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ujjawal on 12/18/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class SalesforceConfig {
  String userName;
  String password;
  String consumerKey;
  String consumerSecret;
  String grantType;
  String loginInstanceDomain;
  String apiVersion;
  boolean enabled;
}
