# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import datetime
import logging
import splunklib.client as client
import splunklib.results as results
from time import sleep

"""
Wraps the Splunk python sdk to read from Splunk
"""


class SplunkSource(object):
    logger = logging.getLogger(__name__)
    logging.basicConfig(level=logging.INFO)

    def __init__(self, host, port, user, password):
        """

        :param host: splunk server host
        :param port: splunk server port
        :param user: splunk user name
        :param password: splunk password
        """
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        # Create a Service instance and log in
        self.service = client.connect(
            host=self.host,
            port=self.port,
            username=self.user,
            password=self.password,
            autologin=True)

    def fetchClusteredEvents(self, query, mins, span=1):

        """

        :param query: the splunk query. This will be used with the search
                        splunk command
        :param mins: the backward time window to query
        :return: the clustered events aggregated by minute buckets
        """

        # Get the collection of jobs
        jobs = self.service.jobs

        searchquery_normal = "search " + query + " | bin _time span=1m | cluster showcount=t labelonly=t" \
                                                 "| table _time, _raw,cluster_label, host | " \
                                                 "stats latest(_raw) as _raw " \
                                                 "count as cluster_count by _time,cluster_label,host"

        if span > mins:
            print("span is > than mins. Setting span to mins")
            span = mins

        now = datetime.datetime.now().replace(second=0)
        print(now)
        raw = []
        for i in reversed(range(mins / span)):

            print(i)

            earliest = now - datetime.timedelta(minutes=(i + 1) * span)
            latest = (earliest + datetime.timedelta(minutes=span - 1, seconds=59))

            print(earliest.strftime('%Y-%m-%dT%H:%M:%S') + ' ' + latest.strftime('%Y-%m-%dT%H:%M:%S'))

            kwargs_normalsearch = {
                "earliest_time": earliest.strftime('%Y-%m-%dT%H:%M:%S'),
                "latest_time": latest.strftime('%Y-%m-%dT%H:%M:%S')}

            job = self.service.jobs.create(searchquery_normal, **kwargs_normalsearch)

            # A normal search returns the job's SID right away, so we need to poll for completion
            while True:
                while not job.is_ready():
                    pass
                stats = {"isDone": job["isDone"],
                         "doneProgress": float(job["doneProgress"]) * 100,
                         "scanCount": int(job["scanCount"]),
                         "eventCount": int(job["eventCount"]),
                         "resultCount": int(job["resultCount"])}

                status = ("\r%(doneProgress)03.1f%%   %(scanCount)d scanned   "
                          "%(eventCount)d matched   %(resultCount)d results") % stats

                self.logger.info(status)
                if stats["isDone"] == "1":
                    self.logger.info("\n\nDone!\n\n")
                    break
                sleep(2)
            # Get the results
            reader = results.ResultsReader(job.results(segmentation='none'))
            for result in reader:
                if isinstance(result, dict):
                    print(result.get('_time'))
                    raw.append(result)
            job.cancel()
            self.logger.info('\n')
        return raw


############# Uncomment to test class ##################
#splunkSource = SplunkSource('ec2-52-54-103-49.compute-1.amazonaws.com', 8089, 'admin', 'W!ngs@Splunk')
#result = splunkSource.fetchClusteredEvents('*exception*', 1440, 100)
#with open('wings.json', 'w') as out:
#    out.write(json.dumps(result))
