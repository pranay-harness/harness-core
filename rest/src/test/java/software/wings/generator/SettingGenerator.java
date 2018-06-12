package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingAttribute.Category.CLOUD_PROVIDER;
import static software.wings.beans.SettingAttribute.Category.CONNECTOR;
import static software.wings.beans.SettingAttribute.Category.SETTING;
import static software.wings.generator.SettingGenerator.Settings.AWS_TEST_CLOUD_PROVIDER;
import static software.wings.generator.SettingGenerator.Settings.DEV_TEST_CONNECTOR;
import static software.wings.generator.SettingGenerator.Settings.GITHUB_TEST_CONNECTOR;
import static software.wings.generator.SettingGenerator.Settings.TERRAFORM_TEST_GIT_REPO;
import static software.wings.utils.WingsTestConstants.HARNESS_ARTIFACTORY;
import static software.wings.utils.WingsTestConstants.HARNESS_DOCKER_REGISTRY;
import static software.wings.utils.WingsTestConstants.HARNESS_JENKINS;
import static software.wings.utils.WingsTestConstants.HARNESS_NEXUS;
import static software.wings.utils.WingsTestConstants.HARNESS_NEXUS_THREE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Account;
import software.wings.beans.AwsConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.generator.AccountGenerator.Accounts;
import software.wings.generator.SecretGenerator.SecretName;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.WingsTestConstants;

@Singleton
public class SettingGenerator {
  @Inject AccountGenerator accountGenerator;
  @Inject SecretGenerator secretGenerator;

  @Inject SettingsService settingsService;
  @Inject WingsPersistence wingsPersistence;

  public enum Settings {
    AWS_TEST_CLOUD_PROVIDER,
    DEV_TEST_CONNECTOR,
    HARNESS_JENKINS_CONNECTOR,
    GITHUB_TEST_CONNECTOR,
    TERRAFORM_TEST_GIT_REPO,
    HARNESS_BAMBOO_CONNECTOR,
    HARNESS_NEXUS_CONNECTOR,
    HARNESS_NEXU3_CONNECTOR,
    HARNESS_ARTIFACTORY_CONNECTOR,
    HARNESS_DOCKER_REGISTRY
  }

  public SettingAttribute ensurePredefined(Randomizer.Seed seed, Settings predefined) {
    switch (predefined) {
      case AWS_TEST_CLOUD_PROVIDER:
        return ensureAwsTest(seed);
      case DEV_TEST_CONNECTOR:
        return ensureDevTest(seed);
      case HARNESS_JENKINS_CONNECTOR:
        return ensureHarnessJenkins(seed);
      case GITHUB_TEST_CONNECTOR:
        return ensureGithubTest(seed);
      case TERRAFORM_TEST_GIT_REPO:
        return ensureTerraformTestGitRepo(seed);
      case HARNESS_BAMBOO_CONNECTOR:
        return ensureHarnessBamboo(seed);
      case HARNESS_NEXUS_CONNECTOR:
        return ensureHarnessNexus(seed);
      case HARNESS_NEXU3_CONNECTOR:
        return ensureHarnessNexus3(seed);
      case HARNESS_ARTIFACTORY_CONNECTOR:
        return ensureHarnessArtifactory(seed);
      case HARNESS_DOCKER_REGISTRY:
        return ensureHarnessDocker(seed);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private SettingAttribute ensureAwsTest(Randomizer.Seed seed) {
    final Account account = accountGenerator.ensurePredefined(seed, Accounts.GENERIC_TEST);
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CLOUD_PROVIDER)
            .withName(AWS_TEST_CLOUD_PROVIDER.name())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(account.getUuid())
            .withValue(AwsConfig.builder()
                           .accessKey("AKIAIQHVMR7P5UESAUJQ")
                           .secretKey(secretGenerator.decryptToCharArray(new SecretName("aws_playground_secret_key")))
                           .accountId(account.getUuid())
                           .build())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureDevTest(Randomizer.Seed seed) {
    final Account account = accountGenerator.randomAccount();

    final SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SETTING)
            .withAccountId(account.getUuid())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withName(DEV_TEST_CONNECTOR.name())
            .withValue(aHostConnectionAttributes()
                           .withConnectionType(SSH)
                           .withAccessType(KEY)
                           .withAccountId(account.getUuid())
                           .withUserName("ubuntu")
                           .withKey(("-----BEGIN RSA PRIVATE KEY-----\n"
                               + "MIIEpQIBAAKCAQEA1Bxs1dMQSD25VBrTVvMvTFwTagL+N9qKAptxdBBRvxFm9Kfv\n"
                               + "TsZAibtfFgXa71gy7id+uMDPGQEHtIeXXvzkYPq/MPltVooWhouadGzrOr1hVDHM\n"
                               + "UGwDGQrpy7XyZPfHKjjGmNUd+B27zDon5RtOZbCbRCvevoPnCvTtItfSFLiF/mE+\n"
                               + "q///1jpyf6jLPz/vpARLr2VoZDNvxhU/RdJOSXQVkxEQKzDUMTsgCTZkh1xc9Nb1\n"
                               + "gfDvd1BfJv6l+2nh2sWmRSy72lbupxDcUG5CUPD4V/ka9duVfGKmylo9QooiW5ER\n"
                               + "0qa0lCHGbzil8xRZwR4gqAct7YU8da1FEBWGlQIDAQABAoIBAQCWz8MeYRxRkPll\n"
                               + "cFFVoEC/9TOki44/HjZEVktbb4L/7Bqc146SHumiREP+P5mD1d0YcaJrMEPPjmjx\n"
                               + "FfstgXfL8FziMGZqQnJzpWzjXNH/iMlb+LBBehrVwmmq+qnm2jmUrpud7OGLGXD+\n"
                               + "a1cUUc7zBJfQ57RPFy++HZlBzdvD+IcPuVqyyQoS6f0PzGrC3nuqsqYKjmAoOJsx\n"
                               + "kLuKS59QJ/HXEGJtduw0UvjfQS4l3qebbFAcImIldZ0kVumhIlxcpes6kqZw8dfH\n"
                               + "dZOndMujWYaJIxRhLHwla+myE6p3eneVg15EcBj9PGKHZkXrmk7Jlt+2j6PVQikw\n"
                               + "Z7HJDwThAoGBAPW1dGbR5ml1wYQnqwLUp9TtMmZqFMC3gMgNXd3NIJkyK1vM0rZs\n"
                               + "qokZB2SxyXwCHw+FjUG9WT/Jahy/Pk1D4cBxGgO5CqK+GjON27tn+HcSjt20ZUnl\n"
                               + "dRhsEIyau034ecIR6zsyHXxJxcU1+yfMp1DD8u0n1wq8OWo3HRH6pn+JAoGBANz+\n"
                               + "ukC8TAF/WnTXaLrYR2KB9KmbD9KmdUT0289xafFIlF8WFdz6baZCXIXmo/oiOURv\n"
                               + "bPnJEqZHsowfdky6m8CHC3zsH6GZDrRP03qj1rHxgu5LP5Na4dHXRB03/dg5nZSV\n"
                               + "mfkFI3swI+9nC0g49g0djT/aqleLbezPUrdRcd+tAoGBAIVtzVFMqOgaF0Vx2S8H\n"
                               + "VkCNsnHlJ3Hj9J4ujAu3qf0nPl5yovaHmjArFFW9KiIacM2YA7ZwYbf+443K2MVS\n"
                               + "mJRNlwfwg3MO8uGOJoXllwrqXATPQrXXUjg57t674/0actxNqMUTmOl2klxezQ22\n"
                               + "2CFG13Orz943iqJAXZv21lWpAoGBAI6+LdnAhit1ch0EQg5lwn4bSMgAc1Dx2c9H\n"
                               + "hW9RZ0fFRKjCYC7Sxt5cAN0wY3wefPT6L96LhPNIXkhpzgSziATsdXwkHC5J6ZiH\n"
                               + "8yZFC1j2kUaP7imkyzW6ILHqx5jRZjpiAwk4y3k3WA67dSsaN7uy+dhjyiEv2znZ\n"
                               + "lCj6f14lAoGAWGMSz05Ugzk7W7XWDkbM+I3K1nCRX7Dws32dWmyPNoEGy+x8sCcu\n"
                               + "9XdXmwNc7akrF8jG8Zk/0qwlfvYh4kSRQr037sdQpB1HrSAP4LeVSeJZFohi1QZG\n"
                               + "lcqQrz6/ZvrHFG/VTrr1JOGSNlKmmptsk9IQAm0nedOh+rWx63w+kJQ=\n"
                               + "-----END RSA PRIVATE KEY-----\n")
                                        .toCharArray())
                           .build())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureGithubTest(Randomizer.Seed seed) {
    final Account account = accountGenerator.ensurePredefined(seed, Accounts.GENERIC_TEST);

    final SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CONNECTOR)
            .withAccountId(account.getUuid())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withName(GITHUB_TEST_CONNECTOR.name())
            .withValue(aHostConnectionAttributes()
                           .withConnectionType(SSH)
                           .withAccessType(KEY)
                           .withAccountId(account.getUuid())
                           .withUserName("test-harness")
                           .withKey(("-----BEGIN RSA PRIVATE KEY-----\n"
                               + "MIIJJwIBAAKCAgEAlcUv1ZYt6F9uyhQ/siGxVcDSR+DKoqd9Ohw9oJxrqc05NGU0\n"
                               + "aB++SHbu7FpluoMU8ZH8qnBtdUVLDwN1MN06hTgOUysaBN9CEixFahxC7UuNFF+g\n"
                               + "q+svodr8Ytu+h4MPHIc6UizoAmSrdYTRcOQNtD8cfYyQ9wJepk9kJUJn8dZT6GrZ\n"
                               + "OSGKXtTjEn8bg3MBGYD/TLGXctUMhDRzuEcT47YPuX2whZdHw4t35oruWI1QBuDi\n"
                               + "aa1X04SzDia3VQpABggjsaMyo3a9Yb89QCjrcLLAJacHAnMkTz9K4dhTcsekX3cm\n"
                               + "AHBzG4uAVJxs85X3nvBE3UgheImDLItNBS6x6KlXIcSJNclZyIgWm5TYexAOuOKb\n"
                               + "vyULOmzDxNrhN7hoZRFt/zxxma74xef3yC9dVwdzq63tWdPlbZc2ECs4fYQGwhD7\n"
                               + "YpDoeOoVRZlTqyAWT7vO/6FJ6s7rBa/tfLtXHluBsywQG6NvOfyk4JcrqUw9bWxw\n"
                               + "AAoBFwNg/QjbfhyLrbGgpDJjQhk4kVatgZjgucL0gzTbEdHw9Jy9ZiJOJTA2Fsoo\n"
                               + "WL9hLQNeENnmpu8rsYaXcDnz8ht7QNvV+T5tmqPw7NrkZ+FuWRNKCHlBk9lJrbXi\n"
                               + "nN/dc5i3rgw3TQJr63pNQLR10HHihRCXvN2jquG9INOv+FQoEN3cCGaW46kCAwEA\n"
                               + "AQKCAgBZdjbzg0ia1F6OUPgXRG70RUCWdN01uYxg6LubM7RP8mloNcfPJp74FCr1\n"
                               + "fa3kciZRgh2GBbAsa57BMhSPgqZRK0HLRiS9okqKJm20S3ti2U1FTTXhW5PRP8ig\n"
                               + "mJg/w/aD2PbqXS6dN1r/0L12jJLvBv/SsuNo2L6G40dzxi3m1Fq8qw5kRPetW8bj\n"
                               + "po9dvpV6kYZuXmqTylU2p0o0Wii0TW0pJL4LnmjcknOlf3mN5aNW6H/2FfgvPcXi\n"
                               + "/xdlscC6maQQmOZHpEbNm3lP2OGCKRQDqbjTKsoWmDz3mMuH9V7nM3m8q27mljaq\n"
                               + "0+F0wzDtfKH19/8eTC3Rrsfr/49++68vJrFN9cQbUaZ/u/59ufktZkAMz+GfPtew\n"
                               + "Te3nxkR85cBAZGiLO/Yy7j6b5dImRCxD51gV0wvZtkLOB/gh42/5XiuAM+diJHj+\n"
                               + "3zuq09qGGxr8A/tUKGh+cR1Uwehkc38kMrjSUZ1vfdxd3OxxoBKF31OTDLBI26lA\n"
                               + "A+JBVIf00i+xgI5UZ4Pn/p8uyANvPaqmnWdXgcP2+6tXwr034QU5HcAc1IhETDgp\n"
                               + "d591iUg3Mkqf5Y2lfCj7ax8nP+amdpi9joyFwXaEG9FFusJJ6AmjtIaFPgOC+8UA\n"
                               + "jfSv0pn01l4w7fJj5Sgmt6KriAei5yU2woXbaiCz3tdN5VmqAQKCAQEAxfVyrH51\n"
                               + "iZtTuHZjDa7EAfXc0DLHqK7HHzzb8V9ZrOrpW9sJNK/cne4kvSDSWfp+xVQ1MA+n\n"
                               + "jlMv0BeYn9WFlgXVxcPPKnQT2l1IvmUet6Vi1lYfsBbIV/FUJgpmHGuvhI2OqLka\n"
                               + "5WXYVnFT5tiV1OpNvyjcZkD1uDwgOKqzdHex3Gv/6JvLfkyY3EDzSP16svFDGbbe\n"
                               + "mfRT2JNHLdyjP5P0f1htoM+v3bTCX6tgMdPi7vSnZXUToKKEUxXDSczvdMCWfKAu\n"
                               + "y+0Qc60O7j72AFprpTWEd/RRZkpthRWNNQAFGhRBzV5svVmOJ+epvu/hjtWbcrU0\n"
                               + "BUpmZswnOoZy6QKCAQEAwa7D/P+Vd6mTRFErXF+zOEv9PLr9nsgv5B/fiylF1NhD\n"
                               + "SrY9j6JMl1iS8pp4KyhDhdk6hwV0hNKuxAkZxSsLQ10u00VzN4e+pXVNeoe8GnSx\n"
                               + "PvNvubJ4T1Q1AqcTnhuX/JPsUFoCwppQjZt++UAgrdEnBzf3GQOJAMoplecVg9xW\n"
                               + "UypMYhtlv1nc4NZXjlGE/P8l+ViF2BAjBy3BCOEbGVWQBkHxbqthMb5GKs9z6MlF\n"
                               + "GoEtUGvVHC9asl06J08c3wHam2+fk7OdNzcAYhELeQD39X8oPa/hdF06cJ1rFPiZ\n"
                               + "lLif0XGVKNbCNyVHvQPMDV6eQAXxczRxxw0uNYrywQKCAQA+417u1a3ZbXMHYvTM\n"
                               + "3/x88vKXYcp0GDJCBj+JStVeTbKc79TY3BWmRoV9X/PzidTEM6BCCHcei+bgoN5n\n"
                               + "yJmLs4baMP6bagz0jjYR5mX/yZbAqNOgSVyUM6KcAym1VbCI17++ci/NOLGlpJMW\n"
                               + "/y96WIUieYSrJ2/oe5Fw7ynbkjr634SJDRV0pjjZn/ip830LZAIBJUtKdYg9gWxX\n"
                               + "cYSbKhG+cwVA1Tn1oPsvEW6Z5cpuR2L2pF775SVMj9lelLBt0tD7/pdfSYy0cwIP\n"
                               + "Rgk4y4DvFzViNke1y2dHpDUb5Um5oz3UVG8PKGg03S/b7LEng0zDADXqiFxHxAui\n"
                               + "WBwhAoIBAGhJig44MGr2S+2FJyvd+8pcYEbLCLTuZZsX7m4oEwnASLt51Tv5z/PO\n"
                               + "JZGryvix/Wmcxf7d2ReLub6kh9O6kZ38FrJ7usYzuVuuBcHsRESagLW8rnP2vQE5\n"
                               + "/jEndVC02umNXLkHPmU6YhVdnMjo1q/A5prkb5BwbtyV/j5Q5yzQ/0pYhDhvOCII\n"
                               + "aP5Ha22eR1VSrUfNeoQUbEf5Qu9dBhJmF97GsxE8BmGaan2ypl65wRO8aoHbDizW\n"
                               + "8qcHu8BewNTzUppPSEVneUe5veMP+nV1KA5wIWLVyTe22zi4CmiIU2nY33UPC5mp\n"
                               + "yXmYAUCtQroQMHWYvaGio4Dif/ckkgECggEAXOQIUXrW4dIl3EFdlzVpEikORtIn\n"
                               + "XDBCsOG5uF/Qv1LTzfVUEr31gufTK99ZOap1yAuzZXQa9d7M4AfnFUE27IyUmxmQ\n"
                               + "2TSOHPK0dMpajp1jRmJnmTsIlT6Ym/yillfc0DalQyUlCN6j5xw4leICpMd2OvN/\n"
                               + "5qiLAWeGnD+keduNr1Z3/vC3fDuTHkyLUQTuQNACfPmZit6ceh7Xqq3hvFXnDBUA\n"
                               + "HCnY0LMoorW1f431dZOeNyGg4dV6L49qIATs83u4csb7IzOFDQdX64ILc+M8Kp36\n"
                               + "Mlof5keGgZeMwhQjc2f9EwA804XHQZwslSeK7wEygEO7uLp5zzbggYvGYQ==\n"
                               + "-----END RSA PRIVATE KEY-----\n")
                                        .toCharArray())
                           .build())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureTerraformTestGitRepo(Randomizer.Seed seed) {
    SettingAttribute githubKey = ensurePredefined(seed, GITHUB_TEST_CONNECTOR);

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CONNECTOR)
            .withName(TERRAFORM_TEST_GIT_REPO.name())
            .withAppId(githubKey.getAppId())
            .withEnvId(githubKey.getEnvId())
            .withAccountId(githubKey.getAccountId())
            .withValue(GitConfig.builder()
                           .repoUrl("https://github.com/wings-software/terraform-test.git")
                           //.sshSettingId(githubKey.getUuid())
                           .username("test-harness")
                           .password("g30rG3#22H@rness#33".toCharArray())
                           .branch("master")
                           .accountId(githubKey.getAccountId())
                           .build())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureHarnessJenkins(Randomizer.Seed seed) {
    final Account account = accountGenerator.ensurePredefined(seed, Accounts.GENERIC_TEST);

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName(HARNESS_JENKINS)
            .withCategory(CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(JenkinsConfig.builder()
                           .accountId(account.getUuid())
                           .jenkinsUrl("https://jenkins.wings.software")
                           .username("wingsbuild")
                           .password(secretGenerator.decryptToCharArray(new SecretName("harness_jenkins")))
                           .authMechanism(Constants.USERNAME_PASSWORD_FIELD)
                           .build())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureHarnessBamboo(Randomizer.Seed seed) {
    final Account account = accountGenerator.ensurePredefined(seed, Accounts.GENERIC_TEST);
    SettingAttribute bambooSettingAttribute =
        aSettingAttribute()
            .withName(WingsTestConstants.HARNESS_BAMBOO)
            .withCategory(Category.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(BambooConfig.builder()
                           .accountId(account.getUuid())
                           .bambooUrl("http://ec2-18-208-86-222.compute-1.amazonaws.com:8085/")
                           .username("wingsbuild")
                           .password(secretGenerator.decryptToCharArray(new SecretName("harness_bamboo")))
                           .build())
            .build();
    return ensureSettingAttribute(seed, bambooSettingAttribute);
  }

  private SettingAttribute ensureHarnessNexus(Randomizer.Seed seed) {
    final Account account = accountGenerator.ensurePredefined(seed, Accounts.GENERIC_TEST);
    SettingAttribute nexusSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_NEXUS)
            .withCategory(Category.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(NexusConfig.builder()
                           .accountId(account.getUuid())
                           .nexusUrl("https://nexus2.harness.io")
                           .username("admin")
                           .password(secretGenerator.decryptToCharArray(new SecretName("harness_nexus")))
                           .build())
            .build();
    return ensureSettingAttribute(seed, nexusSettingAttribute);
  }

  private SettingAttribute ensureHarnessNexus3(Randomizer.Seed seed) {
    final Account account = accountGenerator.ensurePredefined(seed, Accounts.GENERIC_TEST);
    SettingAttribute nexus3SettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_NEXUS_THREE)
            .withCategory(Category.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(NexusConfig.builder()
                           .accountId(account.getUuid())
                           .nexusUrl("https://nexus3.harness.io")
                           .username("admin")
                           .password(secretGenerator.decryptToCharArray(new SecretName("harness_nexus")))
                           .build())
            .build();
    return ensureSettingAttribute(seed, nexus3SettingAttribute);
  }

  private SettingAttribute ensureHarnessArtifactory(Randomizer.Seed seed) {
    final Account account = accountGenerator.ensurePredefined(seed, Accounts.GENERIC_TEST);
    SettingAttribute artifactorySettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_ARTIFACTORY)
            .withCategory(Category.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(ArtifactoryConfig.builder()
                           .accountId(account.getUuid())
                           .artifactoryUrl("https://harness.jfrog.io/harness")
                           .username("admin")
                           .password(secretGenerator.decryptToCharArray(new SecretName("harness_artifactory")))
                           .build())
            .build();
    return ensureSettingAttribute(seed, artifactorySettingAttribute);
  }

  private SettingAttribute ensureHarnessDocker(Randomizer.Seed seed) {
    final Account account = accountGenerator.ensurePredefined(seed, Accounts.GENERIC_TEST);
    SettingAttribute dockerSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_DOCKER_REGISTRY)
            .withCategory(Category.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(DockerConfig.builder()
                           .accountId(account.getUuid())
                           .dockerRegistryUrl("https://registry.hub.docker.com/v2/")
                           .username("wingsplugins")
                           .password(secretGenerator.decryptToCharArray(new SecretName("harness_docker_hub")))
                           .build())
            .build();
    return ensureSettingAttribute(seed, dockerSettingAttribute);
  }

  public SettingAttribute exists(SettingAttribute settingAttribute) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .filter(SettingAttribute.ACCOUNT_ID_KEY, settingAttribute.getAccountId())
        .filter(SettingAttribute.APP_ID_KEY, settingAttribute.getAppId())
        .filter(SettingAttribute.ENV_ID_KEY, settingAttribute.getEnvId())
        .filter(SettingAttribute.CATEGORY_KEY, settingAttribute.getCategory())
        .filter(SettingAttribute.NAME_KEY, settingAttribute.getName())
        .get();
  }

  public SettingAttribute ensureSettingAttribute(Randomizer.Seed seed, SettingAttribute settingAttribute) {
    EnhancedRandom random = Randomizer.instance(seed);

    SettingAttribute.Builder builder = aSettingAttribute();

    if (settingAttribute != null && settingAttribute.getAccountId() != null) {
      builder.withAccountId(settingAttribute.getAccountId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (settingAttribute != null && settingAttribute.getAppId() != null) {
      builder.withAppId(settingAttribute.getAppId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (settingAttribute != null && settingAttribute.getEnvId() != null) {
      builder.withEnvId(settingAttribute.getEnvId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (settingAttribute != null && settingAttribute.getCategory() != null) {
      builder.withCategory(settingAttribute.getCategory());
    } else {
      throw new UnsupportedOperationException();
    }

    if (settingAttribute != null && settingAttribute.getName() != null) {
      builder.withName(settingAttribute.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    SettingAttribute existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    if (settingAttribute != null && settingAttribute.getValue() != null) {
      builder.withValue(settingAttribute.getValue());
    } else {
      throw new UnsupportedOperationException();
    }

    return settingsService.forceSave(builder.build());
  }
}
