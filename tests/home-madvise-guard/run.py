#!/usr/bin/env python3
"""Compile the actual home guard and verify its discard and refresh behavior."""
from pathlib import Path
import os
import shlex
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
ENTRY = ROOT / 'app/src/main/cpp/targets/home/tweaks/entry.cpp'
source = ENTRY.read_text()
start = source.index('using MadviseFn =')
end = source.index('\nvoid InstallMadviseGuard()', start)
production = source[start:end]
fixture = r'''
#include "nativehook/page_guard.h"
#include <cassert>
#include <cerrno>
#include <cstdio>
#include <cstring>
#include <mutex>
#include <utility>
#include <vector>
struct AppliedPatch { uintptr_t address; };
'''
tests = r'''
struct Discard {
    uintptr_t begin;
    size_t bytes;
    int advice;
    bool operator==(const Discard&) const = default;
};
std::vector<Discard> discards;
int failAt = 0;
int record(void* address, size_t bytes, int advice) {
    discards.push_back({reinterpret_cast<uintptr_t>(address), bytes, advice});
    if (failAt > 0 && discards.size() == static_cast<size_t>(failAt)) {
        errno = EIO;
        return -1;
    }
    return 0;
}
int main() {
    const size_t page = nhk::host_page_size();
    const uintptr_t base = page * 4096;
    const uintptr_t protectedPage = base + page;
    g_madviseOriginal = record;
    RefreshProtectedRanges({{protectedPage + 4}, {protectedPage + 8}});
    assert(nhk::add_protected_page(protectedPage));
    struct Request { uintptr_t begin; size_t bytes; int advice; };
    const Request requests[] = {
        {base, page * 3, MADV_DONTNEED},
        {base, page + 1, MADV_DONTNEED},
        {base, page * 2 + 7, MADV_DONTNEED},
        {protectedPage, 1, MADV_DONTNEED},
        {protectedPage, page, MADV_DONTNEED},
        {protectedPage, page + 1, MADV_DONTNEED},
        {base, page, MADV_DONTNEED},
        {base + 1, page * 3, MADV_DONTNEED},
        {0, page, MADV_DONTNEED},
        {protectedPage, 0, MADV_DONTNEED},
        {base, page * 3, MADV_NORMAL},
        {UINTPTR_MAX - page + 1, page * 2, MADV_DONTNEED},
    };
    for (const Request& request : requests) {
        for (int failure : {0, 1, 2}) {
            failAt = failure;
            discards.clear();
            errno = 0;
            const int expected = nhk::guarded_madvise(reinterpret_cast<void*>(request.begin),
                request.bytes, request.advice, record);
            const auto expectedDiscards = discards;
            const int expectedErrno = errno;
            discards.clear();
            errno = 0;
            const int actual = HookMadvise(reinterpret_cast<void*>(request.begin), request.bytes, request.advice);
            assert(actual == expected);
            assert(errno == expectedErrno);
            assert(discards == expectedDiscards);
        }
    }
    failAt = 0;
    // Reverted patches must release protection instead of retaining global registrations.
    RefreshProtectedRanges({});
    discards.clear();
    assert(HookMadvise(reinterpret_cast<void*>(base), page * 3, MADV_DONTNEED) == 0);
    assert(discards.size() == 1 && discards[0].bytes == page * 3);
    g_madviseOriginal = nullptr;
    assert(HookMadvise(reinterpret_cast<void*>(base), page, MADV_DONTNEED) == -1);

#if defined(__linux__)
    // Verify real kernel behavior: only the patched middle page retains its bytes.
    void* mapping = mmap(nullptr, page * 3, PROT_READ | PROT_WRITE,
        MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    assert(mapping != MAP_FAILED);
    auto* bytes = static_cast<unsigned char*>(mapping);
    std::memset(bytes, 0x5a, page * 3);
    RefreshProtectedRanges({{reinterpret_cast<uintptr_t>(bytes + page + 4)}});
    g_madviseOriginal = madvise;
    assert(HookMadvise(mapping, page * 3, MADV_DONTNEED) == 0);
    for (size_t i = 0; i < page * 3; ++i) {
        assert(bytes[i] == (i >= page && i < page * 2 ? 0x5a : 0));
    }
    RefreshProtectedRanges({});
    assert(HookMadvise(bytes + page, 1, MADV_DONTNEED) == 0);
    for (size_t i = page; i < page * 2; ++i) assert(bytes[i] == 0);
    assert(munmap(mapping, page * 3) == 0);
#endif
    std::puts("Home MADV_DONTNEED guard regressions: PASS");
}
'''
with tempfile.TemporaryDirectory(prefix='hc-home-madvise-') as temporary:
    work = Path(temporary)
    cpp = work / 'test.cpp'
    cpp.write_text(fixture + production + tests)
    executable = work / 'test'
    command = shlex.split(os.environ.get('CXX', 'clang++'))
    subprocess.run([*command, '-std=c++20', '-O2', '-Wall', '-Wextra', '-Werror',
                    '-I', str(ROOT / 'app/src/main/cpp'), str(cpp), '-pthread',
                    '-o', str(executable)], check=True)
    subprocess.run([str(executable)], check=True, timeout=15)
