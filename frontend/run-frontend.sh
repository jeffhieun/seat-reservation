#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="${SCRIPT_DIR}/.vite.pid"

COLOR_RESET="\033[0m"
COLOR_BLUE="\033[1;34m"
COLOR_GREEN="\033[1;32m"
COLOR_YELLOW="\033[1;33m"
COLOR_RED="\033[1;31m"

log_info() { echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $*"; }
log_success() { echo -e "${COLOR_GREEN}[OK]${COLOR_RESET} $*"; }
log_warning() { echo -e "${COLOR_YELLOW}[WARN]${COLOR_RESET} $*"; }
log_error() { echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $*" >&2; }

RUN_TESTS=false
RUN_BUILD=false
RUN_VERIFY=false
RUN_BACKGROUND=false
SUCCESS=false

cleanup() {
  local exit_code=$?
  if [[ "${SUCCESS}" == "true" ]]; then
    return
  fi

  if [[ -f "${PID_FILE}" ]]; then
    local pid
    pid="$(cat "${PID_FILE}" 2>/dev/null || true)"
    if [[ -n "${pid}" ]]; then
      kill "${pid}" >/dev/null 2>&1 || true
    fi
    rm -f "${PID_FILE}" >/dev/null 2>&1 || true
  fi
  exit "${exit_code}"
}
trap cleanup EXIT

require_command() {
  local command_name="$1"
  local hint="$2"
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    log_error "Missing command '${command_name}'. ${hint}"
    exit 1
  fi
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --with-tests) RUN_TESTS=true ;;
      --with-build) RUN_BUILD=true ;;
      --verify) RUN_VERIFY=true ;;
      --background) RUN_BACKGROUND=true ;;
      *)
        log_error "Unknown option: $1"
        exit 1
        ;;
    esac
    shift
  done
}

install_dependencies_if_needed() {
  if [[ -d "${SCRIPT_DIR}/node_modules" ]]; then
    log_info "node_modules already exists. Skipping npm install."
    return
  fi

  log_info "Installing frontend dependencies..."
  npm install
  log_success "Frontend dependencies installed."
}

run_frontend_tests() {
  log_info "Running frontend tests..."
  npm test -- --run

  log_info "Running critical frontend flow tests..."
  npm test -- --run \
    src/pages/LoginPage.test.jsx \
    src/pages/RegisterPage.test.jsx \
    src/pages/SeatsPage.test.jsx \
    src/components/ReservationTable.test.jsx \
    src/pages/PaymentPage.test.jsx \
    src/pages/PaymentProcessingPage.test.jsx \
    src/pages/PaymentFailedPage.test.jsx \
    src/api/authApi.test.js \
    src/api/reservationApi.test.js \
    src/api/paymentApi.test.js
  log_success "Frontend tests passed."
}

run_frontend_build() {
  log_info "Building frontend..."
  npm run build
  log_success "Frontend build passed."
}

wait_for_frontend() {
  local attempt
  for attempt in $(seq 1 60); do
    if curl -fsS "http://localhost:3000/" >/dev/null 2>&1; then
      log_success "Frontend is reachable at http://localhost:3000"
      return
    fi
    sleep 1
  done

  log_error "Frontend did not become reachable on port 3000."
  exit 1
}

start_frontend() {
  if [[ -f "${PID_FILE}" ]]; then
    local existing_pid
    existing_pid="$(cat "${PID_FILE}" 2>/dev/null || true)"
    if [[ -n "${existing_pid}" ]] && kill -0 "${existing_pid}" >/dev/null 2>&1; then
      log_warning "Stopping existing Vite process (${existing_pid})."
      kill "${existing_pid}" >/dev/null 2>&1 || true
    fi
    rm -f "${PID_FILE}" >/dev/null 2>&1 || true
  fi

  if [[ "${RUN_BACKGROUND}" == "true" ]]; then
    log_info "Starting Vite in background..."
    nohup npm run dev -- --host 0.0.0.0 --port 3000 >"${SCRIPT_DIR}/.vite.log" 2>&1 &
    echo "$!" > "${PID_FILE}"
    wait_for_frontend
    return
  fi

  log_info "Starting Vite in foreground..."
  npm run dev -- --host 0.0.0.0 --port 3000
}

print_summary() {
  cat <<EOF

Frontend ready:
  URL: http://localhost:3000
EOF
}

main() {
  parse_args "$@"

  require_command "npm" "Install Node.js/npm first."
  require_command "curl" "Install curl first."

  cd "${SCRIPT_DIR}"
  install_dependencies_if_needed

  if [[ "${RUN_TESTS}" == "true" ]]; then
    run_frontend_tests
  fi

  if [[ "${RUN_BUILD}" == "true" ]]; then
    run_frontend_build
  fi

  start_frontend

  if [[ "${RUN_VERIFY}" == "true" ]]; then
    wait_for_frontend
  fi

  print_summary
  SUCCESS=true
}

main "$@"
