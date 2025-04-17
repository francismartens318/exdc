import hashlib
import sys

def main():
    if len(sys.argv) != 2:
        print("Usage: python sha256-hash.py <password>")
        sys.exit(1)
    
    password = sys.argv[1]
    hash_object = hashlib.sha256(password.encode())
    hex_dig = hash_object.hexdigest()
    print(f"SHA256 hash: \n{hex_dig}\n")

if __name__ == "__main__":
    main()