variable "deployment" {
  type = string
}

locals {
  name_prefix = join("_", ["x", "mongo", var.deployment])
  widgets_1 = join(",", data.template_file.widget_template_1.*.rendered)
  widgets_2 = join(",", data.template_file.widget_template_2.*.rendered)
  widgets_3 = join(",", data.template_file.widget_template_3.*.rendered)
}
