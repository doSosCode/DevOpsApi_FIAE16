provider "kubernetes" {
  config_path    = pathexpand("~/.kube/config")
  config_context = "minikube"
}

resource "kubernetes_namespace_v1" "training" {
  metadata {
    name = var.namespace
    labels = {
      environment = "training"
      managed-by  = "opentofu"
      application = var.application_name
    }
  }
}
