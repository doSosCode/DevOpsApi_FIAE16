output "namespace" {
  value = kubernetes_namespace_v1.training.metadata[0].name
}
