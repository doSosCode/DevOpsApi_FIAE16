variable "namespace" {
  type        = string
  description = "Kubernetes-Namespace"
  default     = "training"
}

variable "application_name" {
  type        = string
  description = "Name der Anwendung"
  default     = "task-api"
}
