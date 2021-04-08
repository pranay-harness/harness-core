package io.harness.secretmanagerclient.dto.awskms;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

import java.util.Set;

@Getter
@Setter
@Builder
@FieldNameConstants(innerTypeName = "AwsKmsStsCredentialConfigKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.secretmanagerclient.dto.awskms.AwsKmsStsCredentialConfig")
public class AwsKmsStsCredentialConfig implements AwsKmsCredentialSpecConfig {
  private Set<String> delegateSelectors;//TODO: Shashank: Shoudl we delete?
  private String roleArn;
  private String externalName;
}
