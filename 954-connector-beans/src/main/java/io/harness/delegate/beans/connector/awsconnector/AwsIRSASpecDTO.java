package io.harness.delegate.beans.connector.awsconnector;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDP)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AwsConstants.IRSA)
@ApiModel("AwsIRSA")
public class AwsIRSASpecDTO implements AwsCredentialSpecDTO {}
