#!/usr/bin/env bash
set -Eeuo pipefail

service="${1:?service name is required}"
compose_dir="${2:?compose directory is required}"
image="${3:?image is required}"
aws_region="${4:?AWS region is required}"
env_parameter="${5:?SecureString parameter name is required}"
service_name="${6:-$service}"
compose_file="${compose_dir}/docker-compose.yml"

if [[ ! -f "$compose_file" ]]; then
  echo "Compose file not found: ${compose_file}" >&2
  exit 1
fi

env_file="$(sudo mktemp "${compose_dir}/.env.XXXXXX")"
trap 'sudo rm -f "$env_file"' EXIT

sudo aws ssm get-parameter \
  --region "$aws_region" \
  --name "$env_parameter" \
  --with-decryption \
  --query 'Parameter.Value' \
  --output text |
  sudo sh -c 'umask 077; cat > "$1"' sh "$env_file"
sudo mv "$env_file" "${compose_dir}/.env"
trap - EXIT

cd "$compose_dir"
instance_name="$(hostname)"
private_ip="$(hostname -I | awk '{print $1}')"
compose_env=(
  "IMAGE=$image"
  "IMAGE_NAME=$image"
  "SERVICE_NAME=$service_name"
  "INSTANCE_NAME=$instance_name"
  "PRIVATE_IP=$private_ip"
  "TEMPO_OTLP_ENDPOINT=${TEMPO_OTLP_ENDPOINT:-}"
  "LOKI_PUSH_URL=${LOKI_PUSH_URL:-}"
  "PROMETHEUS_REMOTE_WRITE_URL=${PROMETHEUS_REMOTE_WRITE_URL:-}"
)
sudo env "${compose_env[@]}" docker compose -f "$compose_file" pull "$service"
sudo env "${compose_env[@]}" docker compose -f "$compose_file" up \
  -d \
  --no-deps \
  --wait \
  --wait-timeout 120 \
  "$service"

echo "${service} is running with ${image}."
