/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.tasklet.reader;

import io.harness.batch.processing.dao.intfc.PublishedMessageDao;
import io.harness.ccm.commons.entities.events.PublishedMessage;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PublishedMessageReader {
  private String accountId;
  private String messageType;
  private Long startTime;
  private Long endTime;
  private int batchSize;
  private PublishedMessageDao publishedMessageDao;

  public PublishedMessageReader(PublishedMessageDao publishedMessageDao, String accountId, String messageType,
      Long startTime, Long endTime, int batchSize) {
    this.accountId = accountId;
    this.messageType = messageType;
    this.startTime = startTime;
    this.endTime = endTime;
    this.batchSize = batchSize;
    this.publishedMessageDao = publishedMessageDao;
  }

  public List<PublishedMessage> getNext() {
    List<PublishedMessage> publishedMessageList =
        publishedMessageDao.fetchPublishedMessage(accountId, messageType, startTime, endTime, batchSize);
    if (!publishedMessageList.isEmpty()) {
      Long firstStartTime = publishedMessageList.get(0).getCreatedAt();
      startTime = publishedMessageList.get(publishedMessageList.size() - 1).getCreatedAt();
      if (firstStartTime.equals(startTime)) {
        log.info("Incrementing start Date by 1ms {} {} {} {}", publishedMessageList.size(), startTime, endTime,
            messageType, accountId);
        startTime = startTime + 1;
      }
    }
    return publishedMessageList;
  }
}
