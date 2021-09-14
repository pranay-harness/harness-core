/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

/**
 * Created by mehulkasliwal on 2020-04-17.
 */
public interface MicrosoftTeamsNotificationService {
  int sendMessage(String message, String webhookUrl);
}
