# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import DataHandler as dh
import logging
import numpy as np
import pandas as pd
import time
from collections import OrderedDict
from keras.layers import LSTM, Dense, TimeDistributed, Dropout
from keras.models import Sequential
from sources.MetricTemplate import MetricTemplate

log_format = "%(asctime)-15s %(levelname)s %(message)s"
logging.basicConfig(level=logging.INFO, format=log_format)
logger = logging.getLogger(__name__)


class LSTMAnomalyDetector(object):

    def __init__(self, options, metric_template, control_txns, test_txns):
        self._options = options
        self.metric_template = MetricTemplate(metric_template)
        self.metric_names = self.metric_template.get_metric_names()
        self.raw_control_txns = control_txns
        self.raw_test_txns = test_txns
        self.min_rpm = options.min_rpm

    def get_training_set(self, txn_name, control_txn_data_dict):
        metrics_in_df = []
        metrics_out_df = []
        existing_metrics = []
        for metric_ind, metric_name in enumerate(self.metric_names):
            control_data_dict = dh.get_metrics_data(metric_name, self.metric_template, control_txn_data_dict)
            if dh.validate(txn_name, metric_name, control_data_dict, type='control'):
                metric_shifted_in_data = []
                metric_shifted_out_data = []
                ## number of days is first dimension, number of time point is second dimension, number of host is 3rd dimension of data
                for host_ind in range(len(control_data_dict['host_names'])):
                    host_data = control_data_dict['data'][:, :, host_ind]
                    host_data = host_data[~np.isnan(host_data).all(axis=1)]
                    # this for loop is for different days
                    for data in host_data:
                        data = data - np.nanmean(data)
                        metric_host_df = pd.DataFrame(data, columns=[metric_name])
                        shifted_in_df, shifted_out_df = format_data(metric_host_df, time_steps=self._options.time_steps,
                                                                    dropnan=True)
                        metric_shifted_in_data.append(shifted_in_df.values)
                        metric_shifted_out_data.append(shifted_out_df.values)
                ## appending shifted dataframes of all control hosts for each metric
                metrics_in_df.append(np.concatenate(metric_shifted_in_data, axis=0))
                metrics_out_df.append(np.concatenate(metric_shifted_out_data, axis=0))
                existing_metrics.append(metric_name)
        train_in_data = np.stack(metrics_in_df, axis=2) if metrics_in_df else np.array([])
        train_out_data = np.stack(metrics_out_df, axis=2) if metrics_in_df else pd.DataFrame()
        return dict(train_X=train_in_data, train_Y=train_out_data, metrics=existing_metrics)

    def prediction(self, txn_ind, txn_name, control_txn_data_dict, test_txn_data_dict, result, txns_count):
        response = {'results': {}, 'max_risk': -1, 'control_avg': -1, 'test_avg': -1}
        training = self.get_training_set(txn_name, control_txn_data_dict)
        model = train_lstm(training['train_X'], training['train_Y'], self._options.time_steps, self._options.epochs,
                           self._options.batch_size, self._options.val_percent)
        list_metrics_data = []
        existing_metric = []
        for metric_ind, metric_name in enumerate(self.metric_names):
            test_data_dict = dh.get_metrics_data(metric_name, self.metric_template, test_txn_data_dict)
            if metric_name in training['metrics']:
                if dh.validate(txn_name, metric_name, test_data_dict, type='test'):
                    # test data should be recorded just for last day
                    day_test_data = np.squeeze(test_data_dict['data'], axis=0)
                    list_metrics_data.append(np.transpose(day_test_data))
                    existing_metric.append(metric_name)
        test_metric_3d_data = np.stack(list_metrics_data, axis=2)
        # test data after deployment
        test_metric_3d_data[np.isnan(test_metric_3d_data)] = 0
        new_predicted_data = test_metric_3d_data[:, :self._options.prediction_mins, :]
        start = self._options.prediction_mins - self._options.time_steps
        end = self._options.prediction_mins
        while (end < self._options.total_time + 1):
            test_x = test_metric_3d_data[:, start:end, :]
            mean_x = np.mean(test_x, axis=1)[:, np.newaxis, :]
            test_x = test_x - mean_x
            y_hat = model.predict(test_x) + mean_x
            new_predicted_data = np.concatenate([new_predicted_data, y_hat], axis=1)
            start += self._options.time_steps
            end += self._options.time_steps
        result[txn_name] = OrderedDict({})
        for metric_ind, metric in enumerate(existing_metric):
            for host_ind, host_name in enumerate(test_data_dict['host_names']):
                response['results'][host_name] = {}
                response['results'][host_name]['control_data'] = new_predicted_data[host_ind,
                                                                 :self._options.total_time + 1, metric_ind].tolist()
                response['results'][host_name]['test_data'] = test_metric_3d_data[host_ind, :,
                                                              metric_ind].tolist()
            ''' Use numbers for dictionary keys to avoid a failure on the Harness manager side
                when saving data to MongoDB. MongoDB does not allow 'dot' chars in the key names for
                dictionaries, and transactions or metric names can contain the dot char.
                So use numbers for keys and stick the name inside the dictionary. '''
            if txn_ind not in result['transactions']:
                result['transactions'][txn_ind] = dict(txn_name=txn_name, metrics={})

            result['transactions'][txn_ind]['metrics'][metric_ind] = response
            txns_count += 1
        return 0

    def analyze(self):
        """
         analyze all transaction / metric combinations
        """
        start_time = time.time()
        result = {'transactions': {}}

        control_txn_groups = dh.group_txns(self._options.total_time, self.metric_names, self.raw_control_txns,
                                           self.metric_template, self._options.min_rpm, self._options.num_days)
        test_txn_groups = dh.group_txns(self._options.total_time, self.metric_names, self.raw_test_txns,
                                        self.metric_template, self._options.min_rpm, self._options.num_days)

        txns_count = 0
        if len(control_txn_groups) == 0 or len(test_txn_groups) == 0:
            logger.warn(
                "No control or test data given for minute " + str(
                    self._options.analysis_minute) + ". Skipping analysis!!")
        else:

            for txn_ind, (txn_name, test_txn_data_dict) in enumerate(test_txn_groups.items()):

                if txn_name in control_txn_groups:
                    control_txn_data_dict = control_txn_groups[txn_name]
                else:
                    control_txn_data_dict = {}
                #if txn_name =='WebTransaction':
                self.prediction(txn_ind,  txn_name, control_txn_data_dict, test_txn_data_dict, result, txns_count)
        logger.info('time taken ' + str(time.time() - start_time) + ' for # txns = ' + str(txns_count))
        return result


def train_lstm(features, target, time_steps, epochs, batch_size, val_percent):
    num_metrics = features.shape[2]
    val_dim = int(features.shape[0] * val_percent)
    val_x = features[:val_dim, :, :]
    train_x = features[val_dim:, :, :]
    val_y = target[:val_dim, :, :]
    train_y = target[val_dim:, :, :]
    model = Sequential()
    model.add(LSTM(features.shape[0], return_sequences=True, input_shape=(time_steps, num_metrics)))
    model.add(Dropout(0.2))
    model.add(LSTM(features.shape[0], return_sequences=True))  # returns (timesteps, batchsize)
    model.add(TimeDistributed(Dense(num_metrics, activation='linear')))  # ouptput shape (none, timesteps, num_metric)
    # model.add(TimeDistributed(Dense(num_metrics, activation='linear'), input_shape=(timesteps, num_metrics)))
    model.compile(loss='mse', optimizer="rmsprop", metrics=['accuracy'])
    model.fit(train_x, train_y,
              batch_size=batch_size, epochs=epochs,
              validation_data=(val_x, val_y))
    return model


def format_data(data, time_steps=1, dropnan=True):
    df = data.fillna(0)
    cols, names = list(), list()
    var_name = df.columns[0]
    # input sequence (t-n, ... t-1)
    for i in range(time_steps, 0, -1):
        cols.append(df.shift(i))
        names += [(var_name + '-%d' % i)]
    # forecast sequence (t, t+1, ... t+n)
    for i in range(0, time_steps):
        cols.append(df.shift(-i))
        if i == 0:
            names += [var_name]
        else:
            names += [(var_name + '+%d' % i)]
    # put it all together
    agg = pd.concat(cols, axis=1)
    agg.columns = names
    # drop rows with NaN values
    if dropnan:
        agg.dropna(inplace=True)
    return agg.iloc[:, :time_steps], agg.iloc[:, time_steps:]
