#!/usr/bin/env python3
import os
import sys
import json
import uuid
import urllib.request
import urllib.error

def build_multipart_formdata(fields, files):
    """
    Build a multipart/form-data body.
    fields: dict of {name: value}
    files: dict of {name: (filename, content_type, file_content_bytes)}
    Returns: (content_type_header, body_bytes)
    """
    boundary = uuid.uuid4().hex
    parts = []
    
    # Add regular fields
    for name, value in fields.items():
        parts.append(f"--{boundary}".encode('utf-8'))
        parts.append(f'Content-Disposition: form-data; name="{name}"'.encode('utf-8'))
        parts.append(b'')
        parts.append(str(value).encode('utf-8'))
        
    # Add file fields
    for name, (filename, content_type, file_bytes) in files.items():
        parts.append(f"--{boundary}".encode('utf-8'))
        parts.append(f'Content-Disposition: form-data; name="{name}"; filename="{filename}"'.encode('utf-8'))
        parts.append(f'Content-Type: {content_type}'.encode('utf-8'))
        parts.append(b'')
        parts.append(file_bytes)
        
    parts.append(f"--{boundary}--".encode('utf-8'))
    parts.append(b'')
    
    body = b'\r\n'.join(parts)
    content_type = f'multipart/form-data; boundary={boundary}'
    return content_type, body

def get_content_type(filename):
    ext = os.path.splitext(filename)[1].lower()
    if ext == '.png':
        return 'image/png'
    elif ext in ('.jpg', '.jpeg'):
        return 'image/jpeg'
    elif ext == '.webp':
        return 'image/webp'
    return 'application/octet-stream'

def send_request(url, fields, files, api_key):
    try:
        content_type, body = build_multipart_formdata(fields, files)
        req = urllib.request.Request(url, data=body)
        req.add_header('Content-Type', content_type)
        req.add_header('X-Admin-Key', api_key)
        
        with urllib.request.urlopen(req) as response:
            res_body = response.read().decode('utf-8')
            return True, json.loads(res_body)
    except urllib.error.HTTPError as e:
        error_msg = e.read().decode('utf-8')
        try:
            return False, json.loads(error_msg)
        except Exception:
            return False, {"error": error_msg or f"HTTP {e.code}"}
    except Exception as e:
        return False, {"error": str(e)}

def main():
    if len(sys.argv) < 2:
        print("Usage:")
        print("  python3 upload_assets.py <base_url> [api_key] [images_dir]")
        print("Example:")
        print("  python3 upload_assets.py http://localhost:8080 default_development_admin_key_123 ./images")
        sys.exit(1)
        
    base_url = sys.argv[1].rstrip('/')
    api_key = sys.argv[2] if len(sys.argv) > 2 else "default_development_admin_key_123"
    images_dir = sys.argv[3] if len(sys.argv) > 3 else "./images"
    
    designs_json_path = os.path.join(images_dir, "designs.json")
    designs_csv_path = os.path.join(images_dir, "designs.csv")
    cards_json_path = os.path.join(images_dir, "cards.json")
    
    print(f"Starting upload process to target: {base_url}")
    print(f"Images source directory: {images_dir}")
    print(f"X-Admin-Key: {'***' + api_key[-4:] if len(api_key) > 4 else '***'}")
    
    # 1. Upload Henna Designs
    designs = []
    if os.path.exists(designs_csv_path):
        print(f"\n--- Loading Henna Designs from CSV '{designs_csv_path}' ---")
        import csv
        try:
            with open(designs_csv_path, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    image_file = row.get("filename") or row.get("image") or row.get("file")
                    name = row.get("name") or row.get("title")
                    design_type = row.get("designType") or row.get("type") or "HAND"
                    complexity = row.get("complexity")
                    tags = row.get("tags")
                    
                    if name and image_file:
                        designs.append({
                            "name": name.strip(),
                            "image": image_file.strip(),
                            "designType": design_type.strip(),
                            "complexity": complexity.strip() if complexity else "Simple",
                            "tags": tags.strip() if tags else ""
                        })
        except Exception as e:
            print(f"Error loading {designs_csv_path}: {e}")
    elif os.path.exists(designs_json_path):
        print(f"\n--- Loading Henna Designs from JSON '{designs_json_path}' ---")
        try:
            with open(designs_json_path, 'r', encoding='utf-8') as f:
                designs = json.load(f)
        except Exception as e:
            print(f"Error loading {designs_json_path}: {e}")
            
    if designs:
        print(f"Found {len(designs)} henna design entries to upload.")
        success_count = 0
        for idx, d in enumerate(designs):
            name = d.get("name")
            image_file = d.get("image")
            
            if not name or not image_file:
                print(f"Skipping entry {idx}: name and image are required.")
                continue
                
            img_path = os.path.join(images_dir, image_file)
            if not os.path.exists(img_path):
                print(f"Skipping '{name}': local image file '{img_path}' not found.")
                continue
                
            with open(img_path, 'rb') as img_f:
                img_bytes = img_f.read()
                
            design_type = d.get("designType") or d.get("type") or "HAND"

            fields = {
                "name": name,
                "designType": design_type,
                "complexity": d.get("complexity", "Simple"),
                "tags": d.get("tags", "")
            }
            files = {
                "file": (image_file, get_content_type(image_file), img_bytes)
            }
            
            url = f"{base_url}/admin/designs"
            success, res = send_request(url, fields, files, api_key)
            if success:
                print(f"✓ Success: '{name}' uploaded. Created ID: {res.get('id')}")
                success_count += 1
            else:
                print(f"✗ Failed: '{name}'. Error: {res.get('error') or res}")
                
        print(f"Henna Designs summary: {success_count}/{len(designs)} uploaded successfully.")
    else:
        print(f"\nInfo: Neither designs.csv nor designs.json was found at '{images_dir}'. Skipping designs upload.")
        
    # 2. Upload Invitation Cards
    if os.path.exists(cards_json_path):
        print("\n--- Uploading Invitation Cards ---")
        try:
            with open(cards_json_path, 'r') as f:
                cards = json.load(f)
        except Exception as e:
            print(f"Error loading {cards_json_path}: {e}")
            cards = []
            
        success_count = 0
        for idx, c in enumerate(cards):
            name = c.get("name")
            image_file = c.get("image")
            
            if not name or not image_file:
                print(f"Skipping entry {idx}: name and image are required.")
                continue
                
            img_path = os.path.join(images_dir, image_file)
            if not os.path.exists(img_path):
                print(f"Skipping '{name}': local image file '{img_path}' not found.")
                continue
                
            with open(img_path, 'rb') as img_f:
                img_bytes = img_f.read()
                
            fields = {
                "name": name,
                "description": c.get("description", ""),
                "price": c.get("price", 100),
                "finish": c.get("finish", "MATTE"),
                "printType": c.get("printType", "DIGITAL"),
                "size": c.get("size", "5x7"),
                "material": c.get("material", "CARDSTOCK"),
                "paperWeight": c.get("paperWeight", 300),
                "minOrderQuantity": c.get("minOrderQuantity", 10),
                "tags": c.get("tags", "")
            }
            files = {
                "file": (image_file, get_content_type(image_file), img_bytes)
            }
            
            url = f"{base_url}/admin/invitation-cards"
            success, res = send_request(url, fields, files, api_key)
            if success:
                print(f"✓ Success: '{name}' uploaded. Created ID: {res.get('id')}")
                success_count += 1
            else:
                print(f"✗ Failed: '{name}'. Error: {res.get('error') or res}")
                
        print(f"Invitation Cards summary: {success_count}/{len(cards)} uploaded successfully.")
    else:
        print(f"\nInfo: cards.json not found at '{cards_json_path}'. Skipping invitation cards upload.")

if __name__ == '__main__':
    main()
