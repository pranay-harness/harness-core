import logging
import os
key = '4ac03b05674fc5c488e3b9b235078d5d'

log_format = "%(asctime)-15s %(levelname)s %(message)s"
logging.basicConfig(level=logging.INFO, format=log_format)
log = logging.getLogger("le")
if os.environ.get('learning_env') and os.environ.get('learning_env') != 'ON_PREM' and os.environ.get('logdna_key'):
    from logdna import LogDNAHandler
    from CustomLogger import CustomLogHandler
    options = {'index_meta': True, 'app': 'Learning_Engine_' + os.environ.get('learning_env')}
    test = LogDNAHandler(os.environ.get('logdna_key'), options)
    clog_handler = CustomLogHandler(test)
    # clogHandler.setFormatter(log_format)
    # clogHandler.setLevel(logging.INFO)
    log.addHandler(clog_handler)

def get_log(name):
    return log

