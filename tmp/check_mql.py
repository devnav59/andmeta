#!/usr/bin/env python3
import sys
import re

def check_file(filename):
    print(f"Checking {filename}...")
    with open(filename, 'r', encoding='utf-8', errors='replace') as f:
        content = f.read()

    # Check balanced braces, brackets, parentheses
    stack = []
    lines = content.split('\n')
    in_block_comment = False

    undeclared_suspects = ["SocketListen", "SocketAccept", "INVALID_SOCKET"]
    errors = []

    for i, line in enumerate(lines, 1):
        # strip string literals and comments
        stripped = line.strip()
        for suspect in undeclared_suspects:
            if suspect in stripped and not stripped.startswith("//"):
                errors.append(f"Line {i}: Found invalid MQL function: {suspect}")

    # Check matching braces
    brace_count = 0
    paren_count = 0
    for i, line in enumerate(lines, 1):
        clean = re.sub(r'//.*', '', line)
        clean = re.sub(r'"([^"\\]|\\.)*"', '""', clean)
        brace_count += clean.count('{') - clean.count('}')
        paren_count += clean.count('(') - clean.count(')')
        if brace_count < 0:
            errors.append(f"Line {i}: Extra closing brace '}}'")
            brace_count = 0
        if paren_count < 0:
            errors.append(f"Line {i}: Extra closing parenthesis ')'")
            paren_count = 0

    if brace_count != 0:
        errors.append(f"End of file: Unmatched open braces (count: {brace_count})")
    if paren_count != 0:
        errors.append(f"End of file: Unmatched open parentheses (count: {paren_count})")

    if errors:
        print("ERRORS FOUND:")
        for err in errors:
            print("  ", err)
        return False
    else:
        print("OK: No syntax balance or banned identifier issues found.")
        return True

if __name__ == '__main__':
    ok1 = check_file('/mql/MetaTrader_Bridge_EA.mq5')
    ok2 = check_file('/mql/MetaTrader_Bridge_EA.mq4')
    if not (ok1 and ok2):
        sys.exit(1)
