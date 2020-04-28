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

public class UpdateSSHCredentialsTest extends GraphQLTest {
  @Inject private AccountGenerator accountGenerator;
  @Inject private OwnerManager ownerManager;
  @Inject SSHCredentialHelper sshCredentialHelper;
  private String updatedSecretName = "updatedSecretName";
  private String updatedUserName = "updatedUserName";
  private int updatedPort = 222;
  private String accountId;
  private String secretId;
  private String updatedPrincipal = "updatedPrincipal";
  private String updatedRealm = "updatedRealm";

  @Before
  public void setup() {
    final OwnerManager.Owners owners = ownerManager.create();
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
    secretId = sshCredentialHelper.createSSHCredential("secretName");
  }
  private String updateMutationInput(String variable) {
    String query = $GQL(/*
  mutation{
      updateSecret(input:%s){
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

  private String getUpdateSSHCredentialInput() {
    String queryVariable = $GQL(/*
  {
      secretType: SSH_CREDENTIAL,
      secretId: "%s",
      sshCredential: {
      name: "%s",
      authenticationScheme: SSH,
      sshAuthentication: {
          port: %d,
          userName: "%s",
          sshAuthenticationMethod: {
          sshCredentialType: SSH_KEY,
          inlineSSHKey: {
              sshKey: "sshKeyUpdated"
            }
          }
      }
      }
  }

  */ secretId, updatedSecretName, updatedPort, updatedUserName);
    return queryVariable;
  }

  private void verifySSHResult(SSHCredentialHelper.SSHResult result) {
    SSHCredentialHelper.SSHCredentialResult ssh = result.getSecret();
    assertThat(ssh.getId()).isNotNull();
    assertThat(ssh.getName()).isEqualTo(updatedSecretName);
    SSHCredentialHelper.SSHAuthenticationType authenticationType = ssh.getAuthenticationType();
    assertThat(authenticationType.getUserName()).isEqualTo(updatedUserName);
    assertThat(authenticationType.getPort()).isEqualTo(updatedPort);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testUpdatingSSHCred() {
    String query = updateMutationInput(getUpdateSSHCredentialInput());
    final QLTestObject qlTestObject = qlExecute(query, accountId);
    final SSHCredentialHelper.SSHResult result =
        JsonUtils.convertValue(qlTestObject.getMap(), SSHCredentialHelper.SSHResult.class);
    verifySSHResult(result);
  }

  private String getkerberosUpdateInput() {
    String queryVariable = $GQL(/*

    {
      secretType: SSH_CREDENTIAL,
      secretId: "%s",
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

*/ secretId, updatedSecretName, updatedPort, updatedPrincipal, updatedRealm);
    return queryVariable;
  }

  private void verifyKerberosResult(SSHCredentialHelper.KerberosResult result) {
    SSHCredentialHelper.KerberosCredentialResult ssh = result.getSecret();
    assertThat(ssh.getId()).isNotNull();
    assertThat(ssh.getName()).isEqualTo(updatedSecretName);
    SSHCredentialHelper.KerberosAuthenticationType authenticationType = ssh.getAuthenticationType();
    assertThat(authenticationType.getPrincipal()).isEqualTo(updatedPrincipal);
    assertThat(authenticationType.getPort()).isEqualTo(updatedPort);
    assertThat(authenticationType.getRealm()).isEqualTo(updatedRealm);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testCreatingKerberosCredWithKeyTabFile() {
    String query = updateMutationInput(getkerberosUpdateInput());
    final QLTestObject qlTestObject = qlExecute(query, accountId);
    final SSHCredentialHelper.KerberosResult result =
        JsonUtils.convertValue(qlTestObject.getMap(), SSHCredentialHelper.KerberosResult.class);
    verifyKerberosResult(result);
  }
}
