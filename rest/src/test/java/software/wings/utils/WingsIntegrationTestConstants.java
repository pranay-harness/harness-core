package software.wings.utils;

import org.apache.commons.lang3.StringUtils;

public interface WingsIntegrationTestConstants {
  String API_BASE = StringUtils.isBlank(System.getenv().get("BASE_HTTP")) ? "https://localhost:9090/api"
                                                                          : "http://localhost:9090/api";
  String adminUserName = "Admin";
  String adminUserEmail = "admin@harness.io";
  char[] adminPassword = "admin".toCharArray();

  String readOnlyUserName = "readonlyuser";
  String readOnlyEmail = "readonlyuser@harness.io";
  char[] readOnlyPassword = "readonlyuser".toCharArray();

  String rbac1UserName = "rbac1";
  String rbac1Email = "rbac1@harness.io";
  char[] rbac1Password = "rbac1".toCharArray();

  String rbac2UserName = "rbac2";
  String rbac2Email = "rbac2@harness.io";
  char[] rbac2Password = "rbac2".toCharArray();

  String defaultUserName = "default";
  String defaultEmail = "default@harness.io";
  char[] defaultPassword = "default".toCharArray();

  String defaultAccountName = "Harness";
  String defaultCompanyName = "Harness";
  String defaultAccountId = "kmpySmUISimoRrJL6NL73w";
  String delegateAccountSecret = "2f6b0988b6fb3370073c3d0505baee59";

  String defaultSshUserName = "ubuntu";
  String defaultSshKey = "-----BEGIN RSA PRIVATE KEY-----\n"
      + "MIIEogIBAAKCAQEArCtMvZebz8vGCUah4C4kThYOAEjrdgaGe8M8w+66jPKEnX1GDXj4mrlIxRxO\n"
      + "ErJTwNirPLhIhw/8mGojcsbc5iY7wK6TThJI0uyzUtPfZ1g8zzcQxh7aMOYso/Nxoz6YtO6HRQhd\n"
      + "rxiFuadVo+RuVUeBvVBiQauZMoESh1vGZ2r1eTuXKrSiStaafSfVzSEfvtJYNWnPguqcuGlrX3yv\n"
      + "sNOlIWzU3YETk0bMG3bejChGAKh35AhZdDO+U4g7zH8NI5KjT9IH7MyKAFxiCPYkNm7Y2Bw8j2eL\n"
      + "DIkqIA1YX0VxXBiCC2Vg78o7TxJ7Df7f3V+Q+Xhtj4rRtYCFw1pqBwIDAQABAoIBAGA//LDpNuQe\n"
      + "SWIaKJkJcqZs0fr6yRe8YiaCaVAoAAaX9eeNh0I05NaqyrHXNxZgt03SUzio1XMcTtxuSc76ube4\n"
      + "nCMF9bfppOi2BzJA3F4MCELXx/raeKRpqX8ms9rNPdW4m8rN+IHQtcGqeMgdBkmKpk9NxwBrjEOd\n"
      + "wNwHRI2/Y/ZCApkQDhRPXfEJXnY65SJJ8Vh1NAm6RuiKXv9+8J1//OHAeRfIXTJI4KiwP2EFHeXF\n"
      + "6K0EBVEb/M2kg81bh7iq2OoDxBVrF1Uozg4KUK2EMoCe5OidcSdD1G8ICTsRQlb9iW5e/c2UeCrb\n"
      + "HGkcmQyvDniyfFmVVymyr0vJTnECgYEA6FsPq4T+M0Cj6yUqsIdijqgpY31iX2BAibrLTOFUYhj3\n"
      + "oPpy2bciREXffMGpqiAY8czy3aEroNDC5c7lcwS1HuMgNls0nKuaPLaSg0rSXX9wRn0mYpasBEZ8\n"
      + "5pxFX44FnqTDa37Y7MqKykoMpEB71s1DtG9Ug1cMRuPftZZQ5qsCgYEAvbBcEiPFyKf5g2QRVA/k\n"
      + "FDQcX9hVm7cvDTo6+Qq6XUwrQ2cm9ZJ+zf+Jak+NSN88GNTzAPCWzd8zbZ2D7q4qAyDcSSy0PR3K\n"
      + "bHpLFZnYYOIkSfYcM3CwDhIFTnb9uvG8mypfMFGZ2qUZY/jbI0/cCctsUaXt03g4cM4Q04peehUC\n"
      + "gYAcsWoM9z5g2+GiHxPXetB75149D/W+62bs2ylR1B2Ug5rIwUS/h/LuVWaUxGGMRaxu560yGz4E\n"
      + "/OKkeFkzS+iF6OxIahjkI/jG+JC9L9csfplByyCbWhnh6UZxP+j9NM+S2KvdMWveSeC7vEs1WVUx\n"
      + "oGV0+a6JDY3Rj0BH70kMQwKBgD1ZaK3FPBalnSFNn/0cFpwiLnshMK7oFCOnDaO2QIgkNmnaVtNd\n"
      + "yf0+BGeJyxwidwFg/ibzqRJ0eeGd7Cmp0pSocBaKitCpbeqfsuENnNnYyfvRyVUpwQcL9QNnoLBx\n"
      + "tppInfi2q5f3hbq7pcRJ89SHIkVV8RFP9JEnVHHWcq/xAoGAJNbaYQMmLOpGRVwt7bdK5FXXV5OX\n"
      + "uzSUPICQJsflhj4KPxJ7sdthiFNLslAOyNYEP+mRy90ANbI1x7XildsB2wqBmqiXaQsyHBXRh37j\n"
      + "dMX4iYY1mW7JjS9Y2jy7xbxIBYDpwnqHLTMPSKFQpwsi7thP+0DRthj62sCjM/YB7Es=\n"
      + "-----END RSA PRIVATE KEY-----";

  String sshKeyName = "Wings Key";

  String SEED_APP_NAME = "Seed App";
  String SEED_SERVICE_WAR_NAME = "War Svc";
  String SEED_SERVICE_DOCKER_NAME = "Docker Svc";

  String SEED_ENV_NAME = "Seed Env";
  String SEED_DC_NAME = "Seed DC";
  String SEED_FAKE_HOSTS = "host0, host1, host2, host3, host4, host5, host6, host7, host7, host9, "
      + "host10, host11, host12, host13, host14, host15, host16, host17, host17, host19, "
      + "host20, host21, host22, host23, host24, host25, host26, host27, host27, host29, "
      + "host30, host31, host32, host33, host34, host35, host36, host37, host37, host39, "
      + "host40, host41, host42, host43, host44, host45, host46, host47, host47, host49, "
      + "host50, host51, host52, host53, host54, host55, host56, host57, host57, host59, "
      + "host60, host61, host62, host63, host64, host65, host66, host67, host67, host69, "
      + "host110, host111, host112, host113, host114, host115, host116, host117, host117, host119, "
      + "host120, host121, host122, host123, host124, host125, host126, host127, host127, host129, "
      + "host130, host131, host132, host133, host134, host135, host136, host137, host137, host139, "
      + "host140, host141, host142, host143, host144, host145, host146, host147, host147, host149, "
      + "host150, host151, host152, host153, host154, host155, host156, host157, host157, host159, "
      + "host160, host161, host162, host163, host164, host165";

  String SEED_FAKE_HOSTS_INFRA_NAME = "Seed Infra";

  String SEED_BASIC_WORKFLOW_NAME = "Seed Basic Workflow";
  String SEED_CANARY_WORKFLOW_NAME = "Seed Canary Workflow";
  String SEED_ROLLING_WORKFLOW_NAME = "Seed Rolling Workflow";

  String HARNESS_KMS = "Harness KMS";

  String DEFAULT_USER_KEY = "DEFAULT_USER_KEY";

  String SEED_APP_KEY = "SEED_APP_KEY";

  String SEED_SERVICE_WAR_KEY = "SEED_SERVICE_WAR_KEY";
  String SEED_SERVICE_DOCKER_KEY = "SEED_SERVICE_DOCKER_KEY";

  String SEED_ENV_KEY = "SEED_ENV_KEY";
  String SEED_DC_KEY = "SEED_DC_KEY";
  String SEED_FAKE_HOSTS_DC_INFRA_KEY = "SEED_FAKE_HOSTS_DC_INFRA_KEY";
  String SEED_SSH_KEY = "SEED_SSH_KEY";

  String SEED_BASIC_WORKFLOW_KEY = "SEED_BASIC_WORKFLOW_KEY";
  String SEED_CANARY_WORKFLOW_KEY = "SEED_CANARY_WORKFLOW_KEY";
  String SEED_ROLLING_WORKFLOW_KEY = "SEED_ROLLING_WORKFLOW_KEY";
}
