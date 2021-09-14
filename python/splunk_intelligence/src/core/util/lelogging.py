# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

import logging
import os
from logging.handlers import RotatingFileHandler

key = '4ac03b05674fc5c488e3b9b235078d5d'

log_format = "%(asctime)-15s %(levelname)s %(message)s"
logging.basicConfig(level=logging.INFO, format=log_format)
log = logging.getLogger("le")
if str(os.environ.get('learning_env')).lower() == 'on_prem':
    folder_path ='logs'
    if not os.path.exists(folder_path):
        os.makedirs(folder_path)
    handler = RotatingFileHandler(os.path.join(folder_path, 'le.log'), maxBytes=8388608, backupCount=10)
    handler.setFormatter(logging.Formatter(log_format))
    log.addHandler(handler)


def get_log(name):
    return log
