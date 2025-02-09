variable "lambda_artifact_bucket" {
  description = "Name of the S3 bucket to store Lambda artifacts"
  type        = string
  default     = "lambda-artifacts-shimopino"
}

variable "lambda_artifact_key" {
  description = "S3 key for the Lambda function artifact"
  type        = string
  default     = "function.zip"
}