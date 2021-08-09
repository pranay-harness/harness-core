variable "deployment" {
  type = string
}

variable "projectId" {
  type = string
}


resource "google_monitoring_dashboard" "cvng_dashboard" {
  dashboard_json = <<EOF

{
  "displayName": "CVNG dashboard - ${var.deployment}",
  "gridLayout": {
    "columns": "2",
    "widgets": [
  {
    "title": "Data collection Task metrics - data_collection_task_total_time",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_DELTA"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/data_collection_task_total_time\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_PERCENTILE_99",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_DELTA"
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
    "title": "Data collection Task metrics - data_collection_task_wait_time",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_DELTA"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/data_collection_task_wait_time\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_PERCENTILE_99",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_DELTA"
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
    "title": "Data collection Task metrics - data_collection_task_running_time",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_DELTA"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/data_collection_task_running_time\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_PERCENTILE_99",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_DELTA"
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
    "title": "Data collection Task metrics - data_collection_task_failed_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_RATE"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/data_collection_task_failed_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_COUNT"
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
    "title": "Data collection Task metrics - data_collection_task_success_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_RATE"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/data_collection_task_success_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_COUNT"
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
    "title": "Task count metrics - verification_job_instance_timeout_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_RATE"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/verification_job_instance_timeout_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_COUNT"
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
    "title": "Task count metrics - verification_job_instance_failed_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_RATE"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/verification_job_instance_failed_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_COUNT"
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
    "title": "Task count metrics - verification_job_instance_success_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_RATE"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/verification_job_instance_success_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_COUNT"
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
    "title": "Task count metrics - verification_job_instance_verification_passed_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_RATE"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/verification_job_instance_verification_passed_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_COUNT"
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
    "title": "Task count metrics - verification_job_instance_verification_failed_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_RATE"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/verification_job_instance_verification_failed_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_COUNT"
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
    "title": "Task count metrics - learning_engine_task_timeout_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_RATE"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/learning_engine_task_timeout_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_COUNT"
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
    "title": "Task count metrics - learning_engine_task_failed_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_RATE"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/learning_engine_task_failed_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_COUNT"
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
    "title": "Task count metrics - learning_engine_task_success_count",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_RATE"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/learning_engine_task_success_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_COUNT"
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
    "title": "Analysis Statemachine Metrics - orchestrator_queue_size",
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
              "filter": "metric.type=\"custom.googleapis.com/opencensus/orchestrator_queue_size\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
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
    "title": "Task duration metrics - verification_job_instance_extra_time",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_DELTA"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/verification_job_instance_extra_time\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_PERCENTILE_99",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_DELTA"
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
    "title": "Task duration metrics - learning_engine_task_total_time",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_DELTA"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/learning_engine_task_total_time\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_PERCENTILE_99",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_DELTA"
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
    "title": "Task duration metrics - learning_engine_task_wait_time",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_DELTA"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/learning_engine_task_wait_time\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_PERCENTILE_99",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_DELTA"
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
    "title": "Task duration metrics - learning_engine_task_running_time",
    "xyChart": {
      "dataSets": [
        {
          "timeSeriesQuery": {
            "apiSource": "DEFAULT_CLOUD",
            "timeSeriesFilter": {
              "aggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_NONE",
                "perSeriesAligner": "ALIGN_DELTA"
              },
              "filter": "metric.type=\"custom.googleapis.com/opencensus/learning_engine_task_running_time\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_PERCENTILE_99",
                "groupByFields": [
                  "metric.label.\"accountId\""
                ],
                "perSeriesAligner": "ALIGN_DELTA"
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
    "title": "CVNG tasks status count - cvng_step_task_non_final_status_count",
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
              "filter": "metric.type=\"custom.googleapis.com/opencensus/cvng_step_task_non_final_status_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
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
    "title": "CVNG tasks status count - cvng_step_task_in_progress_count",
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
              "filter": "metric.type=\"custom.googleapis.com/opencensus/cvng_step_task_in_progress_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
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
    "title": "CVNG tasks status count - analysis_state_machine_non_final_status_count",
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
              "filter": "metric.type=\"custom.googleapis.com/opencensus/analysis_state_machine_non_final_status_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
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
    "title": "CVNG tasks status count - analysis_state_machine_created_count",
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
              "filter": "metric.type=\"custom.googleapis.com/opencensus/analysis_state_machine_created_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
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
    "title": "CVNG tasks status count - analysis_state_machine_running_count",
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
              "filter": "metric.type=\"custom.googleapis.com/opencensus/analysis_state_machine_running_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
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
    "title": "CVNG tasks status count - analysis_state_machine_retry_count",
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
              "filter": "metric.type=\"custom.googleapis.com/opencensus/analysis_state_machine_retry_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
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
    "title": "CVNG tasks status count - analysis_state_machine_transition_count",
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
              "filter": "metric.type=\"custom.googleapis.com/opencensus/analysis_state_machine_transition_count\" resource.type=\"k8s_container\"",
              "secondaryAggregation": {
                "alignmentPeriod": "60s",
                "crossSeriesReducer": "REDUCE_SUM",
                "groupByFields": [
                  "metric.label.\"accountId\""
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
                  "metric.label.\"accountId\""
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
                  "metric.label.\"accountId\""
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
                  "metric.label.\"accountId\""
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
                  "metric.label.\"accountId\""
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
                  "metric.label.\"accountId\""
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
                  "metric.label.\"accountId\""
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
                  "metric.label.\"accountId\""
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
                  "metric.label.\"accountId\""
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
                  "metric.label.\"accountId\""
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
                  "metric.label.\"accountId\""
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