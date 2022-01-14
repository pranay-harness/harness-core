package io.harness.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceNowTicketKeyNG {
  @NotNull String url;
  @NotNull String key;
  @NotNull String ticketType;

  public ServiceNowTicketKeyNG(@NotEmpty String baseUrl, @NotEmpty String key, @NotEmpty String ticketType) {
    this.url = ServiceNowUtils.prepareTicketUrlFromTicketNumber(baseUrl, key, ticketType);
    this.key = key;
    this.ticketType = ticketType;
  }
}
