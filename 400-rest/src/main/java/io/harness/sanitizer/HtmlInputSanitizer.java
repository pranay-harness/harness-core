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
