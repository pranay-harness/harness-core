#!/bin/bash

#kill delegate
kill -9 `cat delegate/delegate.pid`

#kill manager
kill -9 `cat rest/manager.pid`

