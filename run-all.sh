#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${PROJECT_DIR}/backend"
FRONTEND_DIR="${PROJECT_DIR}/frontend"
BACKEND_SCRIPT="${BACKEND_DIR}/run-backend.sh"
FRONTEND_SCRIPT="${FRONTEND_DIR}/run-frontend.sh"
FRONTEND_PID_FILE="${FRONTEND_DIR}/.vite.pid"

COLOR_RESET="\033[0m"
COLOR_BLUE="\033[1;34m"
COLOR_GREEN="\033[1;32m"
COLOR_YELLOW="\033[1;33m"
COLOR_RED="\033[1;31m"

log_info() { echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $*"; }
log_success() { echo -e "${COLOR_GREEN}[OK]${COLOR_RESET} $*"; }
log_warning() { echo -e "${COLOR_YELLOW}[WARN]${COLOR_RESET} $*"; }
log_error() { echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $*" >&2; }

COMPOSE_CMD=()
SUCCESS=false

cleanup() {
  local exit_code=$?

  if [[ "${SUCCESS}" == "true" ]]; then
    return
  fi

  log_warning "Startup failed. Cleaning up project services."
  if [[ ${#COMPOSE_CMD[@]} -gt 0 ]]; then
    "${COMPOSE_CMD[@]}" -f "${BACKEND_DIR}/podman-compose.yml" down -v --remove-orphans >/dev/null 2>&1 || true
  fi

  if [[ -f "${FRONTEND_PID_FILE}" ]]; then
    local pid
    pid="$(cat "${FRONTEND_PID_FILE}" 2>/dev/null || true)"
    if [[ -n "${pid}" ]]; then
      kill "${pid}" >/dev/null 2>&1 || true
    fi
    rm -f "${FRONTEND_PID_FILE}" >/dev/null 2>&1 || true
  fi

  exit "${exit_code}"
}
trap cleanup EXIT

require_command() {
  local command_name="$1"
  local hint="$2"
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    log_error "Missing required command: ${command_name}. ${hint}"
    exit 1
  fi
}

setup_compose_command() {
  if podman compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(podman compose)
    return
  fi

  if command -v podman-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(podman-compose)
    return
  fi

  log_error "Neither 'podman compose' nor 'podman-compose' is available."
  exit 1
}

ensure_podman_machine() {
  if ! podman info >/dev/null 2>&1; then
    log_info "Starting Podman machine..."
    podman machine start
  fi

  local retries=30
  for attempt in $(seq 1 "${retries}"); do
    if podman info >/dev/null 2>&1; then
      log_success "Podman machine is healthy."
      return
    fi
    sleep 1
  done

  log_error "Podman machine did not become healthy."
  exit 1
}

#ensure_podman_machine() {
#  local machine_name
#  machine_name="$(podman machine list --format "{{.Name}}" | head -n1 || true)"
#  if [[ -z "${machine_name}" ]]; then
#    log_error "No Podman machine found. Create one with: podman machine init"
#    exit 1
#  fi
#
#  local running
#  running="$(podman machine list --format "{{if eq .Name \"${machine_name}\"}}{{.Running}}{{end}}" | tr -d '[:space:]')"
#  if [[ "${running}" != "true" ]]; then
#    log_info "Starting Podman machine '${machine_name}'..."
#    podman machine start "${machine_name}"
#  else
#    log_success "Podman machine '${machine_name}' is already running."
#  fi
#
#  local retries=30
#  local attempt
#  for attempt in $(seq 1 "${retries}"); do
#    if podman info >/dev/null 2>&1; then
#      log_success "Podman machine is healthy."
#      return
#    fi
#    sleep 1
#  done
#
#  log_error "Podman machine did not become healthy."
#  exit 1
#}

validate_tools() {
  require_command "podman" "Install Podman first."
  require_command "curl" "Install curl first."
  require_command "jq" "Install jq first."
  require_command "npm" "Install Node.js/npm first."
  require_command "java" "Install Java 21 first."
  if ! command -v gradle >/dev/null 2>&1 && [[ ! -x "${BACKEND_DIR}/gradlew" ]]; then
    log_error "Missing Gradle. Install gradle or ensure backend/gradlew is executable."
    exit 1
  fi
}

print_summary() {
  cat <<EOF

====================================
Seat Reservation environment is ready
====================================
Backend:  http://localhost:8080
Swagger:  http://localhost:8080/swagger-ui/index.html
Health:   http://localhost:8080/actuator/health
Frontend: http://localhost:3000
Database: postgresql://localhost:5432/reservation_db
EOF
}

main() {
  log_info "Validating required tools..."
  validate_tools
  setup_compose_command
  ensure_podman_machine

  [[ -x "${BACKEND_SCRIPT}" ]] || chmod +x "${BACKEND_SCRIPT}"
  [[ -x "${FRONTEND_SCRIPT}" ]] || chmod +x "${FRONTEND_SCRIPT}"

  log_info "Starting backend workflow..."
  "${BACKEND_SCRIPT}" --with-tests --with-build --verify

  log_info "Starting frontend workflow..."
  "${FRONTEND_SCRIPT}" --with-tests --with-build --background --verify

  print_summary
  SUCCESS=true
}

main "$@"
