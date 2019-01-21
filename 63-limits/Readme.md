# Limits

## Overview
Harness Freemium imposes need for limits on resource usage and actions. 
This provides a library to support those use cases.

**Where are limits configured?**

* Default Limits are configured in code
See `DefaultLimitsService` class

* Override values are written in database in allowedLimits collection.