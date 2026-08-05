variable "namespace" {
  type        = string
  description = "Kubernetes-Namespace"
  default     = "default"
}

variable "application_name" {
  type        = string
  description = "Name der Anwendung"
  default     = "task-api"
}
