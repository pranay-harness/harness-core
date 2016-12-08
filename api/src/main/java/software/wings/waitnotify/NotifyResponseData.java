package software.wings.waitnotify;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by peeyushaggarwal on 6/24/16.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface NotifyResponseData {}
