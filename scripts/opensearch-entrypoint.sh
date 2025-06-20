#!/bin/bash

# OpenSearch entrypoint script that installs required plugins before starting
set -e

echo "=== OpenSearch Plugin Installation ==="

# Set plugin binary path
PLUGIN_BIN="/usr/share/opensearch/bin/opensearch-plugin"

# Function to check if a plugin is installed
is_plugin_installed() {
    local plugin_name="$1"
    $PLUGIN_BIN list | grep -q "^$plugin_name$"
}

# Function to install a plugin if not already installed
install_plugin_if_needed() {
    local plugin_name="$1"
    echo "Checking plugin: $plugin_name"
    
    if is_plugin_installed "$plugin_name"; then
        echo "✓ Plugin $plugin_name is already installed"
    else
        echo "Installing plugin: $plugin_name"
        $PLUGIN_BIN install --batch "$plugin_name"
        echo "✓ Plugin $plugin_name installed successfully"
    fi
}

# Function to install zentity plugin from file
install_zentity_plugin() {
    local plugin_file="/opt/zentity/releases/zentity-1.8.3-opensearch-2.17.0.zip"
    
    echo "Checking zentity plugin..."
    
    if is_plugin_installed "zentity"; then
        echo "✓ Zentity plugin is already installed"
    else
        if [ -f "$plugin_file" ]; then
            echo "Installing zentity plugin from: $plugin_file"
            $PLUGIN_BIN install --batch "file://$plugin_file"
            echo "✓ Zentity plugin installed successfully"
        else
            echo "⚠ Zentity plugin file not found at: $plugin_file"
            echo "  Skipping zentity plugin installation"
        fi
    fi
}

# Install required plugins
echo "Installing required OpenSearch plugins..."
install_plugin_if_needed "analysis-icu"
install_plugin_if_needed "analysis-phonetic"

# Install zentity plugin
install_zentity_plugin

echo "=== Plugin installation complete ==="
echo "Installed plugins:"
$PLUGIN_BIN list

echo "=== Starting OpenSearch ==="

# Execute the original OpenSearch entrypoint with all arguments
exec /usr/share/opensearch/opensearch-docker-entrypoint.sh "$@" 