package io.harness.secret;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.GraphQLTest;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import io.harness.testframework.graphql.QLTestObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;

public class CreateSSHCredentialTest extends GraphQLTest {
  @Inject private AccountGenerator accountGenerator;
  @Inject private OwnerManager ownerManager;
  private String secretName = "tests";
  private String userName = "ubuntu";
  private int port = 5986;
  private String accountId;
  private String path = "path";
  private String principal = "principal";
  private String realm = "realm";

  @Before
  public void setup() {
    final OwnerManager.Owners owners = ownerManager.create();
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
  }

  private String createMutationInput(String variable) {
    String query = $GQL(/*
  mutation{
      createSecret(input:%s){
          clientMutationId
          secret{
          ... on WinRMCredential{
            name
            id
            port
            skipCertCheck
            useSSL
            userName
            authenticationScheme
        }
        ... on SSHCredential{
            id
            name
            authenticationType{
              ... on SSHAuthentication{
                  userName
                  port
             }
              ... on KerberosAuthentication{
                  port
                  realm
                  principal
              }
             }
        }
      }
    }
  }
 */ variable);
    return query;
  }

  private String getSSHInlineCredentialInput() {
    String queryVariable = $GQL(/*
  {
      secretType: SSH_CREDENTIAL,
      sshCredential: {
      name: "%s",
      authenticationScheme: SSH,
      sshAuthentication: {
          port: %d,
          userName: "%s",
          sshAuthenticationMethod: {
          sshCredentialType: SSH_KEY,
          inlineSSHKey: {
              sshKey: "sshKey"
            }
          }
      }
      }
  }

  */ secretName, port, userName);
    return queryVariable;
  }

  private void verifySSHResult(SSHCredentialHelper.SSHResult result) {
    SSHCredentialHelper.SSHCredentialResult ssh = result.getSecret();
    assertThat(ssh.getId()).isNotNull();
    assertThat(ssh.getName()).isEqualTo(secretName);
    SSHCredentialHelper.SSHAuthenticationType authenticationType = ssh.getAuthenticationType();
    assertThat(authenticationType.getUserName()).isEqualTo(userName);
    assertThat(authenticationType.getPort()).isEqualTo(port);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testCreatingSSHCredWithInlineSSHKey() {
    String query = createMutationInput(getSSHInlineCredentialInput());
    final QLTestObject qlTestObject = qlExecute(query, accountId);
    final SSHCredentialHelper.SSHResult result =
        JsonUtils.convertValue(qlTestObject.getMap(), SSHCredentialHelper.SSHResult.class);
    verifySSHResult(result);
  }

  private String getSSHFilePathInput() {
    String queryVariable = $GQL(/*
{
    secretType: SSH_CREDENTIAL,
    sshCredential: {
    name: "%s",
    authenticationScheme: SSH,
    sshAuthentication: {
        port: %d,
        userName: "%s",
        sshAuthenticationMethod: {
             sshCredentialType: SSH_KEY_FILE_PATH,
                             sshKeyFile: {
             path: "%s"
            }
        }
      }
    }
}

*/ secretName, port, userName, path);
    return queryVariable;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testCreatingSSHCredWithFilePathSSHKey() {
    String query = createMutationInput(getSSHFilePathInput());
    final QLTestObject qlTestObject = qlExecute(query, accountId);
    final SSHCredentialHelper.SSHResult result =
        JsonUtils.convertValue(qlTestObject.getMap(), SSHCredentialHelper.SSHResult.class);
    verifySSHResult(result);
  }

  private String getSSHPasswordInput() {
    String queryVariable = $GQL(/*
{
    secretType: SSH_CREDENTIAL,
    sshCredential: {
    name: "%s",
    authenticationScheme: SSH,
    sshAuthentication: {
        port: %d,
        userName: "%s",
        sshAuthenticationMethod: {
               sshCredentialType: PASSWORD,
                   serverPassword:  {
               password: "password"
            }
        }
      }
    }
}

*/ secretName, port, userName);
    return queryVariable;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testCreatingSSHCredWithPassword() {
    String query = createMutationInput(getSSHPasswordInput());
    final QLTestObject qlTestObject = qlExecute(query, accountId);
    final SSHCredentialHelper.SSHResult result =
        JsonUtils.convertValue(qlTestObject.getMap(), SSHCredentialHelper.SSHResult.class);
    verifySSHResult(result);
  }

  private String getkerberosPasswordInput() {
    String queryVariable = $GQL(/*

    {
      secretType: SSH_CREDENTIAL,
      sshCredential: {
        name: "%s",
        authenticationScheme: KERBEROS,
          kerberosAuthentication: {
          port: %d,
          principal: "%s",
          realm: "%s",
          tgtGenerationMethod: {
            kerberosPassword: {
              password: "password"
            },
            tgtGenerationUsing: PASSWORD
          }
        }
      }
    }

*/ secretName, port, principal, realm);
    return queryVariable;
  }

  private void verifyKerberosResult(SSHCredentialHelper.KerberosResult result) {
    SSHCredentialHelper.KerberosCredentialResult ssh = result.getSecret();
    assertThat(ssh.getId()).isNotNull();
    assertThat(ssh.getName()).isEqualTo(secretName);
    SSHCredentialHelper.KerberosAuthenticationType authenticationType = ssh.getAuthenticationType();
    assertThat(authenticationType.getPrincipal()).isEqualTo(principal);
    assertThat(authenticationType.getPort()).isEqualTo(port);
    assertThat(authenticationType.getRealm()).isEqualTo(realm);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testCreatingKerberosCredWithKeyTabFile() {
    String query = createMutationInput(getKeyTabFileInput());
    final QLTestObject qlTestObject = qlExecute(query, accountId);
    final SSHCredentialHelper.KerberosResult result =
        JsonUtils.convertValue(qlTestObject.getMap(), SSHCredentialHelper.KerberosResult.class);
    verifyKerberosResult(result);
  }

  private String getKeyTabFileInput() {
    String queryVariable = $GQL(/*

{
  secretType: SSH_CREDENTIAL,
  sshCredential: {
    name: "%s",
    authenticationScheme: KERBEROS,
      kerberosAuthentication: {
      port: %d,
      principal: "%s",
      realm: "%s",
      tgtGenerationMethod: {
              keyTabFile: {
                  filePath: "abc"
              },
        tgtGenerationUsing: KEY_TAB_FILE
      }
    }
  }
}

*/ secretName, port, principal, realm);
    return queryVariable;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testCreatingKerberosCredWithPassword() {
    String query = createMutationInput(getkerberosPasswordInput());
    final QLTestObject qlTestObject = qlExecute(query, accountId);
    final SSHCredentialHelper.KerberosResult result =
        JsonUtils.convertValue(qlTestObject.getMap(), SSHCredentialHelper.KerberosResult.class);
    verifyKerberosResult(result);
  }
}
