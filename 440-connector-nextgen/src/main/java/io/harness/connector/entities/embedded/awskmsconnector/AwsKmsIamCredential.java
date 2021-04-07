package io.harness.connector.entities.embedded.awskmsconnector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "AwsKmsIamCredentialKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.awskmsconnector.AwsKmsIamCredential")
public class AwsKmsIamCredential implements AwsKmsCredentialSpec {
  private Set<String> delegateSelectors;//TODO:Shashank: Shoudl we delete?
}
