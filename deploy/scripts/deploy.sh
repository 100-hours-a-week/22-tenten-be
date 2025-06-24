#!/bin/bash
set -e

CONTAINER_NAME=backend-app
IMAGE_INFO_FILE="/home/ubuntu/imageDetail.json"

echo "[1] Load image details from file..."
IMAGE_TAG=$(jq -r .imageTag "$IMAGE_INFO_FILE")
ECR_URI=$(jq -r .ecrRepo "$IMAGE_INFO_FILE")
AWS_REGION=$(jq -r .awsRegion "$IMAGE_INFO_FILE")

echo "[2] Authenticate with ECR..."
aws ecr get-login-password --region "$AWS_REGION" | \
  docker login --username AWS --password-stdin "$ECR_URI"

echo "[3] Stop and remove existing container if exists..."
docker stop $CONTAINER_NAME || true
docker rm $CONTAINER_NAME || true

echo "[4] Pull image from ECR..."
docker pull $ECR_URI:$IMAGE_TAG

echo "[5] Load environment variables from SSM..."


PARAM_KEYS=(
  DISCORD_ERROR_WEBHOOK_URL
  JWT_SECRET
  MYSQL_URL
  MYSQL_PASSWORD
  COOKIE_DOMAIN
  EMAIL
  EMAIL_PASSWORD
  BUCKET_NAME
  AWS_ACCESS_KEY
  AWS_SECRET_KEY
  AI_SERVER_URL
)

DOCKER_ENV_ARGS=""

for KEY in "${PARAM_KEYS[@]}"; do
  VALUE=$(aws ssm get-parameter \
    --name "/kakaobase/backend/$KEY" \
    --with-decryption \
    --region "$AWS_REGION" \
    --query "Parameter.Value" \
    --output text)
  DOCKER_ENV_ARGS+=" -e $KEY=\"$VALUE\""
done

echo "[6] Run new container..."
eval "docker run -d \
  --name $CONTAINER_NAME \
  -e SPRING_PROFILES_ACTIVE=prod \
  $DOCKER_ENV_ARGS \
  -p 8080:8080 \
  $ECR_URI:$IMAGE_TAG"

echo "[✅] Deployment completed. [$ECR_URI:$IMAGE_TAG]"
