package io.harness.secretmanagerclient.dto.awskms;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

import java.util.Set;

@Getter
@Setter
@Builder
@FieldNameConstants(innerTypeName = "AwsKmsIamCredentialConfigKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.secretmanagerclient.dto.awskms.AwsKmsIamCredentialConfig")
public class AwsKmsIamCredentialConfig implements AwsKmsCredentialSpecConfig {
  private Set<String> delegateSelectors;//TODO: Shashank: Shoudl we delete?
}
