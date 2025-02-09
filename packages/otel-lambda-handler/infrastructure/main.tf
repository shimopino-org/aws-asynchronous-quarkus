terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-northeast-1"
}

# S3 Bucket for Lambda artifacts
resource "aws_s3_bucket" "lambda_artifacts" {
  bucket        = var.lambda_artifact_bucket
  force_destroy = true
}

resource "aws_s3_bucket_versioning" "lambda_artifacts" {
  bucket = aws_s3_bucket.lambda_artifacts.id
  versioning_configuration {
    status = "Enabled"
  }
}

# SQS Queue
resource "aws_sqs_queue" "sample_queue" {
  name = "sample-otel-queue"
}

# IAM Role for Lambda
resource "aws_iam_role" "lambda_role" {
  name = "otel_lambda_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

# IAM Policy for Lambda
resource "aws_iam_role_policy_attachment" "lambda_policy" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "lambda_xray" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

resource "aws_iam_role_policy" "lambda_sqs" {
  role = aws_iam_role.lambda_role.name
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = aws_sqs_queue.sample_queue.arn
      }
    ]
  })
}

resource "aws_iam_role_policy" "lambda_s3" {
  role = aws_iam_role.lambda_role.name
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject"
        ]
        Resource = "${aws_s3_bucket.lambda_artifacts.arn}/*"
      }
    ]
  })
}

# CloudWatch Log Group for Lambda
resource "aws_cloudwatch_log_group" "lambda_log_group" {
  name              = "/aws/lambda/otel-sample-function"
  retention_in_days = 14
}

# Lambda Function
resource "aws_lambda_function" "otel_function" {
  s3_bucket     = aws_s3_bucket.lambda_artifacts.id
  s3_key        = var.lambda_artifact_key
  function_name = "otel-sample-function"
  role          = aws_iam_role.lambda_role.arn
  handler       = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  runtime       = "java21"
  timeout       = 30
  memory_size   = 512

  layers = ["arn:aws:lambda:ap-northeast-1:901920570463:layer:aws-otel-java-wrapper-amd64-ver-1-32-0:4"]

  environment {
    variables = {
      JAVA_TOOL_OPTIONS        = "-javaagent:/opt/aws-otel-java-agent.jar"
      OTEL_RESOURCE_ATTRIBUTES = "service.name=otel-lambda-handler"
      OTEL_TRACES_SAMPLER      = "always_on"
    }
  }

  tracing_config {
    mode = "Active"
  }

  depends_on = [
    aws_cloudwatch_log_group.lambda_log_group
  ]
}

# Lambda trigger from SQS
resource "aws_lambda_event_source_mapping" "sqs_trigger" {
  event_source_arn = aws_sqs_queue.sample_queue.arn
  function_name    = aws_lambda_function.otel_function.function_name
  batch_size       = 1
}
