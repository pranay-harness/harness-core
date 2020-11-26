package io.harness.persistence;

import java.util.Date;

public interface ValidUntilAccess {
  String VALID_UNTIL_KEY = "validUntil";

  Date getValidUntil();
}
