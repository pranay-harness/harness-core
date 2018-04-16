package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.generator.SettingGenerator.Settings.AWS_TEST_CLOUD_PROVIDER;
import static software.wings.generator.SettingGenerator.Settings.DEV_TEST_CONNECTOR;
import static software.wings.generator.SettingGenerator.Settings.TERRAFORM_TEST_GIT_REPO;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Account;
import software.wings.beans.AwsConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.generator.AccountGenerator.Accounts;
import software.wings.service.intfc.SettingsService;

@Singleton
public class SettingGenerator {
  @Inject AccountGenerator accountGenerator;

  @Inject SettingsService settingsService;

  public enum Settings {
    AWS_TEST_CLOUD_PROVIDER,
    DEV_TEST_CONNECTOR,
    HARNESS_JENKINS_CONNECTOR,
    TERRAFORM_TEST_GIT_REPO,
  }

  public SettingAttribute ensurePredefined(Randomizer.Seed seed, Settings predefined) {
    switch (predefined) {
      case AWS_TEST_CLOUD_PROVIDER:
        return ensureAwsTest(seed);
      case DEV_TEST_CONNECTOR:
        return ensureDevTest(seed);
      case HARNESS_JENKINS_CONNECTOR:
        return ensureHarnessJenkins(seed);
      case TERRAFORM_TEST_GIT_REPO:
        return ensureTerraformTestGitRepo(seed);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private SettingAttribute ensureAwsTest(Randomizer.Seed seed) {
    final Account account = accountGenerator.randomAccount();
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(Category.CLOUD_PROVIDER)
            .withName(AWS_TEST_CLOUD_PROVIDER.name())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(account.getUuid())
            .withValue(AwsConfig.builder()
                           .accessKey("AKIAJJUEMEKQBYHZCQSA")
                           .secretKey("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".toCharArray())
                           .accountId(account.getUuid())
                           .build())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureDevTest(Randomizer.Seed seed) {
    final Account account = accountGenerator.randomAccount();

    final SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
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

  private SettingAttribute ensureTerraformTestGitRepo(Randomizer.Seed seed) {
    final Account account = accountGenerator.randomAccount();

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName(TERRAFORM_TEST_GIT_REPO.name())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(account.getUuid())
            .withValue(GitConfig.builder()
                           .repoUrl("https://github.com/wings-software/terraform-test.git")
                           .username("george-harness")
                           .branch("master")
                           .accountId(account.getUuid())
                           .build())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureHarnessJenkins(Randomizer.Seed seed) {
    final Account account = accountGenerator.ensurePredefined(seed, Accounts.GENERIC_TEST);

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName("Harness Jenkins")
            .withCategory(Category.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(JenkinsConfig.builder()
                           .accountId(account.getUuid())
                           .jenkinsUrl("https://jenkins.wings.software")
                           .username("wingsbuild")
                           .password("06b13aea6f5f13ec69577689a899bbaad69eeb2f".toCharArray())
                           .build())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
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

    if (settingAttribute != null && settingAttribute.getValue() != null) {
      builder.withValue(settingAttribute.getValue());
    } else {
      throw new UnsupportedOperationException();
    }

    return settingsService.forceSave(builder.build());
  }
}
