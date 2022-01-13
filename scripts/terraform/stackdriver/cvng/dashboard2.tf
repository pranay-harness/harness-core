resource "google_monitoring_dashboard" "cvng_dashboard_2" {
  dashboard_json = <<EOF

{
  "displayName": "CVNG dashboard-2-${var.deployment}",
  "gridLayout": {
    "columns": "2",
    "widgets": [
  {
    "title": "CVNG tasks status count - data_collection_task_non_final_status_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_MEAN"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/data_collection_task_non_final_status_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"environment\""
                ],
                "perSeriesAligner": "ALIGN_MEAN"
              }
            }
          },
          "plotType": "LINE"
        }
      ],
      "timeshiftDuration": "0s",
      "yAxis": {
        "label": "y1Axis",
        "scale": "LINEAR"
      },
      "chartOptions": {
        "mode": "COLOR"
      }
    }
  },
  {
    "title": "CVNG tasks status count - data_collection_task_queued_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_MEAN"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/data_collection_task_queued_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"environment\""
                ],
                "perSeriesAligner": "ALIGN_MEAN"
              }
            }
          },
          "plotType": "LINE"
        }
      ],
      "timeshiftDuration": "0s",
      "yAxis": {
        "label": "y1Axis",
        "scale": "LINEAR"
      },
      "chartOptions": {
        "mode": "COLOR"
      }
    }
  },
  {
    "title": "CVNG tasks status count - data_collection_task_running_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_MEAN"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/data_collection_task_running_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"environment\""
                ],
                "perSeriesAligner": "ALIGN_MEAN"
              }
            }
          },
          "plotType": "LINE"
        }
      ],
      "timeshiftDuration": "0s",
      "yAxis": {
        "label": "y1Axis",
        "scale": "LINEAR"
      },
      "chartOptions": {
        "mode": "COLOR"
      }
    }
  },
  {
    "title": "CVNG tasks status count - data_collection_task_waiting_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_MEAN"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/data_collection_task_waiting_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"environment\""
                ],
                "perSeriesAligner": "ALIGN_MEAN"
              }
            }
          },
          "plotType": "LINE"
        }
      ],
      "timeshiftDuration": "0s",
      "yAxis": {
        "label": "y1Axis",
        "scale": "LINEAR"
      },
      "chartOptions": {
        "mode": "COLOR"
      }
    }
  },
  {
    "title": "CVNG tasks status count - verification_job_instance_non_final_status_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_MEAN"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/verification_job_instance_non_final_status_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"environment\""
                ],
                "perSeriesAligner": "ALIGN_MEAN"
              }
            }
          },
          "plotType": "LINE"
        }
      ],
      "timeshiftDuration": "0s",
      "yAxis": {
        "label": "y1Axis",
        "scale": "LINEAR"
      },
      "chartOptions": {
        "mode": "COLOR"
      }
    }
  },
  {
    "title": "CVNG tasks status count - verification_job_instance_queued_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_MEAN"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/verification_job_instance_queued_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"environment\""
                ],
                "perSeriesAligner": "ALIGN_MEAN"
              }
            }
          },
          "plotType": "LINE"
        }
      ],
      "timeshiftDuration": "0s",
      "yAxis": {
        "label": "y1Axis",
        "scale": "LINEAR"
      },
      "chartOptions": {
        "mode": "COLOR"
      }
    }
  },
  {
    "title": "CVNG tasks status count - verification_job_instance_running_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_MEAN"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/verification_job_instance_running_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"environment\""
                ],
                "perSeriesAligner": "ALIGN_MEAN"
              }
            }
          },
          "plotType": "LINE"
        }
      ],
      "timeshiftDuration": "0s",
      "yAxis": {
        "label": "y1Axis",
        "scale": "LINEAR"
      },
      "chartOptions": {
        "mode": "COLOR"
      }
    }
  },
  {
    "title": "CVNG tasks status count - learning_engine_task_non_final_status_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_MEAN"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/learning_engine_task_non_final_status_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"environment\""
                ],
                "perSeriesAligner": "ALIGN_MEAN"
              }
            }
          },
          "plotType": "LINE"
        }
      ],
      "timeshiftDuration": "0s",
      "yAxis": {
        "label": "y1Axis",
        "scale": "LINEAR"
      },
      "chartOptions": {
        "mode": "COLOR"
      }
    }
  },
  {
    "title": "CVNG tasks status count - learning_engine_task_queued_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_MEAN"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/learning_engine_task_queued_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"environment\""
                ],
                "perSeriesAligner": "ALIGN_MEAN"
              }
            }
          },
          "plotType": "LINE"
        }
      ],
      "timeshiftDuration": "0s",
      "yAxis": {
        "label": "y1Axis",
        "scale": "LINEAR"
      },
      "chartOptions": {
        "mode": "COLOR"
      }
    }
  },
  {
    "title": "CVNG tasks status count - learning_engine_task_running_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_MEAN"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/learning_engine_task_running_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"environment\""
                ],
                "perSeriesAligner": "ALIGN_MEAN"
              }
            }
          },
          "plotType": "LINE"
        }
      ],
      "timeshiftDuration": "0s",
      "yAxis": {
        "label": "y1Axis",
        "scale": "LINEAR"
      },
      "chartOptions": {
        "mode": "COLOR"
      }
    }
  }
]
  }
}


  EOF
}