/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.git;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

public class UsernamePasswordCredentialsProviderWithSkipSslVerify extends CredentialsProvider {
  private String username;
  private char[] password;

  public UsernamePasswordCredentialsProviderWithSkipSslVerify(String username, String password) {
    this(username, password.toCharArray());
  }

  public UsernamePasswordCredentialsProviderWithSkipSslVerify(String username, char[] password) {
    this.username = username;
    this.password = password == null ? null : password.clone();
  }

  @Override
  public boolean isInteractive() {
    return false;
  }

  /**
   * (Copied from JGit)
   * Check if the provider can supply the necessary CredentialItems.
   * Specified by: supports in class CredentialsProvider

   * @param items - the items the application requires to complete authentication.
   * @return true if this CredentialsProvider supports all of the items supplied.
   */
  @Override
  public boolean supports(CredentialItem... items) {
    for (CredentialItem i : items) {
      if (i instanceof CredentialItem.Username) {
        continue;
      } else if (i instanceof CredentialItem.Password) {
        continue;
      } else if (i instanceof CredentialItem.YesNoType) {
        continue;
      }
      if (!(i instanceof CredentialItem.InformationalMessage)) {
        return false;
      }
    }
    return true;
  }

  /**
   * (Copied from JGit)
   * Ask for the credential items to be populated.
   * Specified by: get in class CredentialsProvider

   * @param uri - the URI of the remote resource that needs authentication.
   * @param items - the items the application requires to complete authentication.
   * @return true if the request was successful and values were supplied; false if the user canceled the request and
   * did not supply all requested values.
   * @throws UnsupportedCredentialItem - if one of the items supplied is not supported.
   */
  @Override
  public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
    for (CredentialItem i : items) {
      if (i instanceof CredentialItem.Username) {
        ((CredentialItem.Username) i).setValue(username);
        continue;
      }
      if (i instanceof CredentialItem.Password) {
        ((CredentialItem.Password) i).setValue(password);
        continue;
      }
      if (i instanceof CredentialItem.StringType) {
        if (i.getPromptText().equals("Password: ")) { //$NON-NLS-1$
          ((CredentialItem.StringType) i).setValue(new String(password));
          continue;
        }
      }
      if (i instanceof CredentialItem.YesNoType) {
        ((CredentialItem.YesNoType) i).setValue(true);
        continue;
      }
      if (i instanceof CredentialItem.InformationalMessage) {
        continue;
      }

      throw new UnsupportedCredentialItem(uri, i.getClass().getName() + ":" + i.getPromptText()); //$NON-NLS-1$
    }
    return true;
  }
}
