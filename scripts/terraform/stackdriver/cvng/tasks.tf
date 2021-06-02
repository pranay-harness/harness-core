variable "deployment" {
  type = string
}

variable "projectId" {
  type = string
}


resource "google_monitoring_dashboard" "cvng_tasks_dashboard" {
  dashboard_json = <<EOF

{
  "displayName": "CVNG tasks - ${var.deployment}",
  "gridLayout": {
    "columns": "2",
    "widgets": [
      {
        "title": "Data collection tasks non final status count",
        "xyChart": {
          "dataSets": [
            {
              "timeSeriesQuery": {
                	"timeSeriesQueryLanguage": "fetch k8s_container| metric 'custom.googleapis.com/opencensus/data_collection_task_non_final_status_count' | group_by 1m, [value_data_collection_task_non_final_status_count_mean: mean(value.data_collection_task_non_final_status_count)] | every 1m"
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