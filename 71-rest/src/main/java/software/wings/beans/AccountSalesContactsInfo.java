package software.wings.beans;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
@Builder
public class AccountSalesContactsInfo {
  List<String> salesContacts;
}
