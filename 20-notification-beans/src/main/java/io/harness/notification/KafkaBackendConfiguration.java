package io.harness.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaBackendConfiguration extends NotificationClientBackendConfiguration {
  @JsonProperty("bootstrapServers") String bootstrapServers;
  @JsonProperty("securityProtocol") String securityProtocol;
  @JsonProperty("saslJaasConfig") String saslJaasConfig;
  @JsonProperty("sslEndpointIdentificationAlgorithm") String sslEndpointIdentificationAlgorithm;
  @JsonProperty("saslMechanism") String saslMechanism;
  @JsonProperty("consumerGroupName") String consumerGroupName;
}
