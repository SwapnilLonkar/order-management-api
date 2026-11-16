#!/usr/bin/env bash
#
# seed-demo.sh — restore the OMS demo state (accounts, product catalog,
# and the three documented seed orders) against a running backend.
#
# Safe to re-run: every step checks before it creates, so running it
# against an already-seeded database changes nothing.
#
# Passwords are NEVER stored in this file. They are read from the
# environment — in a codespace they are injected from GitHub Codespaces
# user secrets (see .env.example for the variable names). Locally, copy
# .env.example to .env, fill in the values, and run:  set -a; . ./.env; set +a
#
# Usage:
#   OMS_DEMO_PASSWORD=... OMS_PRIYA_PASSWORD=... OMS_SELLER_PASSWORD=... \
#   OMS_SELLER2_PASSWORD=... OMS_SELLER3_PASSWORD=... OMS_ADMIN_PASSWORD=... \
#   ./scripts/seed-demo.sh
#
# Optional overrides:
#   OMS_API_URL       backend base URL   (default http://localhost:8080)
#   OMS_DB_CONTAINER  Postgres container (default order-management-api-postgres-1)
#
set -euo pipefail

API="${OMS_API_URL:-http://localhost:8080}"
DB_CONTAINER="${OMS_DB_CONTAINER:-order-management-api-postgres-1}"
export OMS_API_URL="$API"

# Fail loudly if any password is missing — never fall back to a default.
: "${OMS_DEMO_PASSWORD:?Set OMS_DEMO_PASSWORD (Codespaces secret or .env)}"
: "${OMS_PRIYA_PASSWORD:?Set OMS_PRIYA_PASSWORD (Codespaces secret or .env)}"
: "${OMS_SELLER_PASSWORD:?Set OMS_SELLER_PASSWORD (Codespaces secret or .env)}"
: "${OMS_SELLER2_PASSWORD:?Set OMS_SELLER2_PASSWORD (Codespaces secret or .env)}"
: "${OMS_SELLER3_PASSWORD:?Set OMS_SELLER3_PASSWORD (Codespaces secret or .env)}"
: "${OMS_ADMIN_PASSWORD:?Set OMS_ADMIN_PASSWORD (Codespaces secret or .env)}"

echo "Waiting for the API at $API ..."
for i in $(seq 1 30); do
  if curl -s -o /dev/null "$API/api/products"; then
    echo "API is up."
    break
  fi
  if [ "$i" = "30" ]; then
    echo "ERROR: API did not answer at $API after 60s. Is the backend running?" >&2
    exit 1
  fi
  sleep 2
done

echo "Step 1/3: accounts"
python3 - <<'PY'
import json, os, sys, urllib.request, urllib.error

API = os.environ["OMS_API_URL"]

def req(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(API + path, data=data, method=method)
    r.add_header("Content-Type", "application/json")
    if token:
        r.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(r) as resp:
            raw = resp.read().decode()
            return resp.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            return e.code, {"raw": raw}

def login(username, password):
    status, body = req("POST", "/api/auth/login", {"username": username, "password": password})
    if status == 200 and body.get("token"):
        return body["token"]
    return None

def ensure_user(username, password, role):
    if login(username, password):
        print(f"  exists:  {username} ({role})")
        return
    status, _ = req("POST", "/api/auth/register",
                    {"username": username, "password": password, "role": role})
    if login(username, password):
        print(f"  created: {username} ({role})")
    else:
        print(f"  ERROR: could not create or log in as {username} (register status {status})")
        sys.exit(1)

ensure_user("demo",    os.environ["OMS_DEMO_PASSWORD"],    "CUSTOMER")
ensure_user("priya",   os.environ["OMS_PRIYA_PASSWORD"],   "CUSTOMER")
ensure_user("seller",  os.environ["OMS_SELLER_PASSWORD"],  "SELLER")
ensure_user("seller2", os.environ["OMS_SELLER2_PASSWORD"], "SELLER")
ensure_user("seller3", os.environ["OMS_SELLER3_PASSWORD"], "SELLER")
ensure_user("admin",   os.environ["OMS_ADMIN_PASSWORD"],   "CUSTOMER")  # promoted in step 2
PY

echo "Step 2/3: promote admin (database role update)"
if docker exec "$DB_CONTAINER" psql -U postgres -d orderdb \
     -c "UPDATE users SET role='ADMIN' WHERE username='admin';" >/dev/null 2>&1; then
  echo "  admin is ADMIN."
else
  echo "  NOTE: could not reach the database container '$DB_CONTAINER'."
  echo "        Run this SQL manually once:"
  echo "        UPDATE users SET role='ADMIN' WHERE username='admin';"
fi

echo "Step 3/3: products and seed orders"
python3 - <<'PY'
import json, os, sys, urllib.request, urllib.error

API = os.environ["OMS_API_URL"]
PW = {
    "demo":    os.environ["OMS_DEMO_PASSWORD"],
    "priya":   os.environ["OMS_PRIYA_PASSWORD"],
    "seller":  os.environ["OMS_SELLER_PASSWORD"],
    "seller2": os.environ["OMS_SELLER2_PASSWORD"],
    "seller3": os.environ["OMS_SELLER3_PASSWORD"],
}

def req(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(API + path, data=data, method=method)
    r.add_header("Content-Type", "application/json")
    if token:
        r.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(r) as resp:
            raw = resp.read().decode()
            return resp.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            return e.code, {"raw": raw}

def login(username):
    status, body = req("POST", "/api/auth/login",
                       {"username": username, "password": PW[username]})
    if status != 200 or not body.get("token"):
        print(f"  ERROR: login failed for {username}")
        sys.exit(1)
    return body["token"]

def items(body):
    if isinstance(body, list):
        return body
    return body.get("content", [])

# name, seller, price, initial stock, description, imageUrls (first = cover)
CATALOG = [
    ("Mechanical Keyboard", "seller", 129.99, 40,
     "Keychron K8 wireless mechanical keyboard with RGB backlight.",
     ["https://thumb.wikimedia.org/wikipedia/commons/thumb/6/68/Lingbao_JIGUANSHI_Mechanical_Gaming_Keyboard.jpg/1280px-Lingbao_JIGUANSHI_Mechanical_Gaming_Keyboard.jpg"]),
    ("Wireless Mouse", "seller", 49.99, 114,
     "Logitech MX Master 3S wireless mouse with quiet clicks and an 8K DPI sensor.",
     ["https://thumb.wikimedia.org/wikipedia/commons/thumb/2/28/2017_Mysz_komputerowa_Logitech_MX_Master.jpg/960px-2017_Mysz_komputerowa_Logitech_MX_Master.jpg",
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f9/Logitech_MX_Master_%2820242297670%29.jpg/960px-Logitech_MX_Master_%2820242297670%29.jpg",
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1f/Logitech_mx_master_%2818491736194%29.jpg/960px-Logitech_mx_master_%2818491736194%29.jpg"]),
    ("USB-C Hub", "seller", 79.99, 29,
     "Anker PowerExpand 8-in-1 USB-C hub with HDMI, USB-A, SD and Ethernet ports.",
     ["https://cbu01.alicdn.com/img/ibank/O1CN01RL0pSv2JCNpRLTydh_!!2210879489385-0-cib.jpg",
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cb/Anker_PowerExpand_8-in-1-2186.jpg/1280px-Anker_PowerExpand_8-in-1-2186.jpg"]),
    ("Laptop Stand", "seller", 59.99, 13,
     "Foldable X-stand laptop stand that raises a laptop for ergonomic viewing.",
     ["https://thumb.wikimedia.org/wikipedia/commons/thumb/4/43/Laptop_stand.jpg/960px-Laptop_stand.jpg"]),
    ("LED Desk Lamp", "seller", 39.99, 25,
     "Bamboo LED desk lamp with adjustable brightness for reading and work.",
     ["https://upload.wikimedia.org/wikipedia/commons/7/71/Concise_bamboo_eye_protection_LED_desk_lamp.jpg"]),
    ("Wireless Charger Pad", "seller", 29.99, 60,
     "Samsung wireless charging pad for Qi-enabled phones and accessories.",
     ["https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1c/Samsung_Wireless_Charger_Pad%2C_IFA_2018%2C_Berlin_%28P1070294%29.jpg/960px-Samsung_Wireless_Charger_Pad%2C_IFA_2018%2C_Berlin_%28P1070294%29.jpg"]),
    ("Headphones", "seller", 49.99, 5,
     "AudioQuest NightHawk Carbon open-back headphones with biocellulose drivers.",
     ["https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1d/AudioQuest_NightHawk_Carbon_headphones_%2833582571334%29.jpg/960px-AudioQuest_NightHawk_Carbon_headphones_%2833582571334%29.jpg",
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/20/AudioQuest_NightHawk_Carbon_headphones_%2834424584065%29.jpg/960px-AudioQuest_NightHawk_Carbon_headphones_%2834424584065%29.jpg",
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6c/AudioQuest_NightHawk_Carbon_headphones_and_Perch_%2833582571464%29.jpg/960px-AudioQuest_NightHawk_Carbon_headphones_and_Perch_%2833582571464%29.jpg"]),
    ("Ergonomic Office Chair", "seller2", 249.99, 8,
     "Herman Miller Aeron ergonomic office chair with breathable mesh support.",
     ["https://upload.wikimedia.org/wikipedia/commons/d/d3/Aeron_Chair.jpg",
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/53/Aeron_Chair_by_Herman_Miller_%289446986497%29.jpg/960px-Aeron_Chair_by_Herman_Miller_%289446986497%29.jpg",
      "https://upload.wikimedia.org/wikipedia/commons/c/ce/Aeron_chair_Brooklyn_Museum.jpg"]),
    ("HD Webcam", "seller2", 69.99, 45,
     "Logitech Brio 301 HD webcam for video calls and streaming.",
     ["https://thumb.wikimedia.org/wikipedia/commons/thumb/7/79/Webcam_%28Logitech_c922%29.jpg/960px-Webcam_%28Logitech_c922%29.jpg",
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/55/Webcam_01.jpg/960px-Webcam_01.jpg"]),
    ("Bluetooth Speaker", "seller2", 89.99, 30,
     "JBL Flip 3 portable Bluetooth speaker with splashproof design.",
     ["https://thumb.wikimedia.org/wikipedia/commons/thumb/6/67/JBL_Flip_3_bluetooth_speaker_%28DSCF2653%29.jpg/960px-JBL_Flip_3_bluetooth_speaker_%28DSCF2653%29.jpg",
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d6/G%C5%82o%C5%9Bnik_JBL_Flip_3.jpg/960px-G%C5%82o%C5%9Bnik_JBL_Flip_3.jpg"]),
    ("Smart Watch", "seller2", 199.99, 20,
     "Samsung Gear S3 Frontier smartwatch with rotating bezel and GPS.",
     ["https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a2/Samsung_Gear_S3.jpg/960px-Samsung_Gear_S3.jpg"]),
    ("Wireless Earbuds", "seller3", 59.99, 80,
     "Samsung Galaxy Buds2 Pro wireless earbuds in lavender with active noise cancelling.",
     ["https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b7/Pair_of_lavender_Samsung_Galaxy_Buds2_Pro.jpg/960px-Pair_of_lavender_Samsung_Galaxy_Buds2_Pro.jpg",
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/65/Samsung_Galaxy_Buds2_Pro_%28Graphite%29.jpg/960px-Samsung_Galaxy_Buds2_Pro_%28Graphite%29.jpg"]),
    ("Portable SSD 1TB", "seller3", 109.99, 35,
     "Samsung T5 portable SSD with 1TB of USB 3.1 storage.",
     ["https://thumb.wikimedia.org/wikipedia/commons/thumb/2/24/Samsung_Portable_SSD_T5%2C_Oosterflank%2C_Rotterdam_%282020%29.jpg/960px-Samsung_Portable_SSD_T5%2C_Oosterflank%2C_Rotterdam_%282020%29.jpg"]),
    ("Noise-Cancelling Headphones", "seller3", 179.99, 25,
     "Sony WH-1000XM3 noise-cancelling wireless headphones with 30-hour battery life.",
     ["https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4b/Sony-WH-1000XM3-kabellose-Bluetooth-Noise-Cancelling-Kopfhoerer.jpg/960px-Sony-WH-1000XM3-kabellose-Bluetooth-Noise-Cancelling-Kopfhoerer.jpg",
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/eb/Sony-WH-1000XM3-kabellose-Bluetooth-Noise-Cancelling-Kopfhoerer.2.jpg/960px-Sony-WH-1000XM3-kabellose-Bluetooth-Noise-Cancelling-Kopfhoerer.2.jpg",
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/df/Sony-WH-1000XM3-kabellose-Bluetooth-Noise-Cancelling-Kopfhoerer.3.jpg/960px-Sony-WH-1000XM3-kabellose-Bluetooth-Noise-Cancelling-Kopfhoerer.3.jpg"]),
]

demo_token = login("demo")
_, body = req("GET", "/api/products", token=demo_token)
by_name = {p["name"]: p["id"] for p in items(body)}

tokens = {}
for name, seller, price, stock, desc, urls in CATALOG:
    if name in by_name:
        print(f"  exists:  {name}")
        continue
    if seller not in tokens:
        tokens[seller] = login(seller)
    status, created = req("POST", "/api/products", {
        "name": name, "description": desc, "price": price,
        "stockQuantity": stock, "imageUrl": urls[0], "imageUrls": urls,
    }, token=tokens[seller])
    if status not in (200, 201) or "id" not in created:
        print(f"  ERROR: creating {name} returned {status}: {created}")
        sys.exit(1)
    by_name[name] = created["id"]
    print(f"  created: {name} (id {created['id']}, {len(urls)} photo(s))")

# Seed orders — only on a fresh database (demo has no orders yet).
_, demo_orders = req("GET", "/api/orders", token=demo_token)
if items(demo_orders):
    print("  orders:  seed orders already present, skipping.")
else:
    def place(customer, product_name, qty):
        token = login(customer)
        status, order = req("POST", "/api/orders",
                            {"items": [{"productId": by_name[product_name], "quantity": qty}]},
                            token=token)
        if status not in (200, 201) or "id" not in order:
            print(f"  ERROR: order for {customer} returned {status}: {order}")
            sys.exit(1)
        return order["id"]

    def set_status(seller, order_id, status_value):
        token = login(seller)
        status, _ = req("PATCH", f"/api/orders/{order_id}/status?status={status_value}",
                        token=token)
        if status != 200:
            print(f"  ERROR: setting order {order_id} to {status_value} returned {status}")
            sys.exit(1)

    o1 = place("demo", "Ergonomic Office Chair", 1)
    set_status("seller2", o1, "CONFIRMED")
    print(f"  order {o1}: demo bought 1x Ergonomic Office Chair -> CONFIRMED")
    o2 = place("demo", "Wireless Earbuds", 2)
    set_status("seller3", o2, "SHIPPED")
    print(f"  order {o2}: demo bought 2x Wireless Earbuds -> SHIPPED")
    o3 = place("priya", "Portable SSD 1TB", 1)
    print(f"  order {o3}: priya bought 1x Portable SSD 1TB -> PENDING")

print("Seed complete.")
PY

echo "Done. Demo state restored (or already present)."
