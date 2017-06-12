import logging
from time import sleep

import splunklib.client as client
import splunklib.results as results

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

    def fetchClusteredEvents(self, query, mins):

        """

        :param query: the splunk query. This will be used with the search
                        splunk command
        :param mins: the backward time window to query
        :return: the clustered events aggregated by minute buckets
        """

        # Get the collection of jobs
        jobs = self.service.jobs

        raw = []
        for i in range(mins):
            searchquery_normal = "search " + query + " | cluster showcount=t " \
                                                     "| table _time, _raw, cluster_label, cluster_count"
            kwargs_normalsearch = {
                "earliest_time": "-" + str(mins + 1) + "m",
                "latest_time": "-" + str(mins) + "m"}
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
                    raw.append(result)
            job.cancel()
            self.logger.info('\n')
            mins = mins - 1
        return raw

############# Uncomment to test class ##################
# splunkSource = SplunkSource('ec2-52-54-103-49.compute-1.amazonaws.com', 8089, 'admin', 'W!ngs@Splunk')
# splunkSource.fetchClusteredEvents('*', 5)
