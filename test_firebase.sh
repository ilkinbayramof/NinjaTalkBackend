#!/bin/bash
# Firebase credentials-ını birbaşa test edir

JSON_FILE="src/main/resources/firebase-adminsdk.json"

echo "=== Firebase Credential Test ==="
echo ""

# JSON-dan lazımi sahələri oxu
CLIENT_EMAIL=$(python3 -c "import json; d=json.load(open('$JSON_FILE')); print(d['client_email'])")
PROJECT_ID=$(python3 -c "import json; d=json.load(open('$JSON_FILE')); print(d['project_id'])")
PRIVATE_KEY_ID=$(python3 -c "import json; d=json.load(open('$JSON_FILE')); print(d['private_key_id'])")

echo "📋 Project ID    : $PROJECT_ID"
echo "📧 Client Email  : $CLIENT_EMAIL"
echo "🔑 Key ID        : $PRIVATE_KEY_ID"
echo ""

# Private key-in düzgün format olduğunu yoxla
PRIVATE_KEY=$(python3 -c "import json; d=json.load(open('$JSON_FILE')); print(d['private_key'])")
if echo "$PRIVATE_KEY" | grep -q "BEGIN PRIVATE KEY"; then
    echo "✅ private_key format doğrudur"
else
    echo "❌ private_key format YANLIŞ!"
fi
echo ""

# JWT token yarat və Google token endpoint-inə göndər
echo "🔄 Google OAuth2 token testi..."
python3 - <<'PYEOF'
import json
import time
import base64
import hashlib
import hmac
import struct
import urllib.request
import urllib.parse

def base64url_encode(data):
    if isinstance(data, str):
        data = data.encode('utf-8')
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode('utf-8')

# Load credentials
with open('src/main/resources/firebase-adminsdk.json') as f:
    creds = json.load(f)

client_email = creds['client_email']
private_key_pem = creds['private_key']
project_id = creds['project_id']

# Create JWT header + payload
header = {"alg": "RS256", "typ": "JWT", "kid": creds['private_key_id']}
now = int(time.time())
payload = {
    "iss": client_email,
    "sub": client_email,
    "aud": "https://oauth2.googleapis.com/token",
    "iat": now,
    "exp": now + 3600,
    "scope": "https://www.googleapis.com/auth/firebase.messaging"
}

header_b64 = base64url_encode(json.dumps(header))
payload_b64 = base64url_encode(json.dumps(payload))
signing_input = f"{header_b64}.{payload_b64}".encode('utf-8')

# Sign with private key
try:
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import padding
    from cryptography.hazmat.backends import default_backend

    private_key = serialization.load_pem_private_key(
        private_key_pem.encode('utf-8'),
        password=None,
        backend=default_backend()
    )
    signature = private_key.sign(signing_input, padding.PKCS1v15(), hashes.SHA256())
    jwt_token = f"{header_b64}.{payload_b64}.{base64url_encode(signature)}"

    # Exchange JWT for access token
    token_url = "https://oauth2.googleapis.com/token"
    data = urllib.parse.urlencode({
        "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "assertion": jwt_token
    }).encode('utf-8')

    req = urllib.request.Request(token_url, data=data, method='POST')
    req.add_header('Content-Type', 'application/x-www-form-urlencoded')

    try:
        with urllib.request.urlopen(req) as resp:
            result = json.loads(resp.read().decode())
            access_token = result.get('access_token', '')
            print(f"✅ Token uğurla alındı! (ilk 20 simvol: {access_token[:20]}...)")
            print(f"   Token type: {result.get('token_type')}")
            print(f"   Expires in: {result.get('expires_in')} saniyə")
    except urllib.error.HTTPError as e:
        error_body = e.read().decode()
        print(f"❌ Token alınmadı! HTTP {e.code}")
        print(f"   Xəta: {error_body}")

except ImportError:
    print("⚠️  'cryptography' paketi yüklü deyil, tam test edilə bilmir")
    print("   pip3 install cryptography")
except Exception as e:
    print(f"❌ Xəta: {e}")
PYEOF
