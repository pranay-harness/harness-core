#!/bin/bash

#kill delegate
pgrep -f "integration-test/wings/delegate" | xargs kill -9

#kill manager
pgrep -f "integration-test/wings/rest" | xargs kill -9

