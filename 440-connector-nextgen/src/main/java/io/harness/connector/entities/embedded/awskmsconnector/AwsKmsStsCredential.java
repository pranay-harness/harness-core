package io.harness.connector.entities.embedded.awskmsconnector;

import com.amazonaws.auth.STSSessionCredentialsProvider;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "AwsKmsStsCredentialKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.awskmsconnector.AwsKmsStsCredential")
public class AwsKmsStsCredential implements AwsKmsCredentialSpec {
  private Set<String> delegateSelectors;//TODO:Shashank: Shoudl we delete?
  private String roleArn;
  private String externalName;
  private int assumeStsRoleDuration = STSSessionCredentialsProvider.DEFAULT_DURATION_SECONDS;
}
