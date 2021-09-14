/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.sanitizer;

import org.apache.commons.text.StringEscapeUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class HtmlInputSanitizer implements InputSanitizer {
  private PolicyFactory htmlPolicy;

  public HtmlInputSanitizer() {
    this.htmlPolicy = new HtmlPolicyBuilder().toFactory();
  }

  public String sanitizeInput(String input) {
    return StringEscapeUtils.unescapeHtml4(this.htmlPolicy.sanitize(input));
  }
}
