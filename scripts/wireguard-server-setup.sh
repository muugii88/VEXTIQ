#!/bin/bash
#═══════════════════════════════════════════════════════════════════
# VEXTIQ WireGuard VPN Server Setup Script
# For Google Cloud VM (Ubuntu 22.04)
#
# Usage: 
#   chmod +x wireguard-server-setup.sh
#   sudo ./wireguard-server-setup.sh
#═══════════════════════════════════════════════════════════════════

set -e

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║     VEXTIQ WireGuard VPN Server Setup                     ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo "[!] Please run as root: sudo ./wireguard-server-setup.sh"
    exit 1
fi

# Get server public IP
SERVER_IP=$(curl -s ifconfig.me)
echo "[OK] Server IP: $SERVER_IP"

# Install WireGuard
echo "[>>] Installing WireGuard..."
apt update
apt install -y wireguard qrencode

# Generate server keys
echo "[>>] Generating server keys..."
cd /etc/wireguard
umask 077
wg genkey | tee server_private.key | wg pubkey > server_public.key
SERVER_PRIVATE=$(cat server_private.key)
SERVER_PUBLIC=$(cat server_public.key)

# Generate client keys (for VEXTIQ)
echo "[>>] Generating client keys..."
wg genkey | tee client_private.key | wg pubkey > client_public.key
CLIENT_PRIVATE=$(cat client_private.key)
CLIENT_PUBLIC=$(cat client_public.key)

# Create server config
echo "[>>] Creating server config..."
cat > /etc/wireguard/wg0.conf << EOF
[Interface]
Address = 10.0.0.1/24
ListenPort = 51820
PrivateKey = $SERVER_PRIVATE

# Enable IP forwarding and NAT
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o ens4 -j MASQUERADE; sysctl -w net.ipv4.ip_forward=1
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o ens4 -j MASQUERADE

# VEXTIQ Client
[Peer]
PublicKey = $CLIENT_PUBLIC
AllowedIPs = 10.0.0.2/32
EOF

# Create client config (for Windows/VEXTIQ)
echo "[>>] Creating client config..."
cat > /etc/wireguard/vextiq-client.conf << EOF
[Interface]
PrivateKey = $CLIENT_PRIVATE
Address = 10.0.0.2/24
DNS = 8.8.8.8, 1.1.1.1

[Peer]
PublicKey = $SERVER_PUBLIC
Endpoint = $SERVER_IP:51820
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
EOF

# Enable IP forwarding permanently
echo "[>>] Enabling IP forwarding..."
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf
sysctl -p

# Start WireGuard
echo "[>>] Starting WireGuard..."
systemctl enable wg-quick@wg0
systemctl start wg-quick@wg0

# Show status
echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║     ✅ WireGuard Server Setup Complete!                   ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""
echo "Server Public Key: $SERVER_PUBLIC"
echo "Server IP: $SERVER_IP:51820"
echo ""
echo "═══════════════════════════════════════════════════════════"
echo "CLIENT CONFIG (Copy this to Windows):"
echo "═══════════════════════════════════════════════════════════"
cat /etc/wireguard/vextiq-client.conf
echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "QR Code for mobile (optional):"
qrencode -t ansiutf8 < /etc/wireguard/vextiq-client.conf
echo ""
echo "[i] Client config saved to: /etc/wireguard/vextiq-client.conf"
echo "[i] Copy the config above to your Windows PC"
echo ""
echo "Google Cloud Firewall Rule (run in Cloud Shell):"
echo "  gcloud compute firewall-rules create wireguard --allow udp:51820"
echo ""
