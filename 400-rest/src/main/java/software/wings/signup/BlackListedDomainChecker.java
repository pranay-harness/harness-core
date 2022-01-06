/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.signup;

import static io.harness.annotations.dev.HarnessModule._950_NG_SIGNUP;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.SignupException;

import software.wings.app.MainConfiguration;

import com.google.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@TargetModule(_950_NG_SIGNUP)
@Slf4j
@Singleton
public class BlackListedDomainChecker {
  private static final String BLACKLISTED_DOMAINS_FILE = "trial/blacklisted-email-domains.txt";
  private Set<String> blacklistedDomains = new HashSet<>();
  private MainConfiguration mainConfiguration;

  public BlackListedDomainChecker(MainConfiguration configuration) {
    log.info("Creating the blackisted filler");
    this.mainConfiguration = configuration;
    populateBlacklistedDomains();
  }

  public void check(String email) {
    if (!mainConfiguration.isBlacklistedEmailDomainsAllowed()) {
      if (blacklistedDomains.contains(getEmailDomain(email))) {
        throw new SignupException("The domain of the email used for trial registration is not allowed.");
      }
    }
  }

  private String getEmailDomain(String email) {
    return email.substring(email.indexOf('@') + 1);
  }

  private void populateBlacklistedDomains() {
    if (blacklistedDomains.isEmpty()) {
      try (InputStream inputStream =
               Thread.currentThread().getContextClassLoader().getResourceAsStream(BLACKLISTED_DOMAINS_FILE);
           BufferedReader bufferedReader =
               new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        String line = bufferedReader.readLine();
        while (isNotEmpty(line)) {
          blacklistedDomains.add(line.trim().toLowerCase());
          line = bufferedReader.readLine();
        }
        log.info("Loaded {} temporary email domains into the blacklist.", blacklistedDomains.size());
      } catch (IOException e) {
        log.error("Failed to read blacklisted temporary email domains from file.", e);
      }
    }
  }
}
