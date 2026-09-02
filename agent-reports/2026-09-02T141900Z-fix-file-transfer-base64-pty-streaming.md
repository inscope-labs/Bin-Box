# Process Report: Fix File Transfer Base64 PTY Streaming and EOF Handling

**Timestamp:** 2026-09-02T14:19:00Z  
**Task Slug:** fix-file-transfer-base64-pty-streaming  

## 1. What was asked
The user reported a transfer failure when uploading via the bottom sheet:
`AfEewe8ACQAAA==base64: invalid inputgzip: stdin: unexpected end of filetar: A lone zero block at 17tar: Child returned status 1`

## 2. Root Cause Analysis
1. The previous implementation streamed Base64 without line breaks (`Base64.NO_WRAP`) as one continuous stream, causing PTY cooked mode / Linux TTY line buffers (`MAX_CANON`) to truncate long lines.
2. It terminated the stream by writing raw ASCII EOT (`0x04`) bytes over the PTY. In interactive terminal sessions, raw EOT bytes sent into stdin of a running sub-process are received as literal characters rather than triggering an OS EOF on standard input. `base64 -d` encountered character `0x04` as invalid Base64 input, failed, and abruptly closed the pipe, leading to `unexpected end of file` in `gzip` and a truncated tar block in `tar`.

## 3. What was actually changed
- **`app/src/main/java/com/inscopelabs/abx/binbox/terminal/transfer/FileTransferEngine.kt`**:
  - Replaced the direct pipe with a Shell Heredoc block: `cat << '$eofMarker' | base64 -d | tar -xzf -`.
  - Switched Base64 formatting to `Base64.DEFAULT` (RFC 2045 standard 76-character line wrapping with `\n`), ensuring compatibility with PTY canonical line buffers.
  - Replaced raw EOT byte sending with clean heredoc end marker termination `\n$eofMarker\n`, allowing `cat` to naturally close stdout and trigger a true OS-level EOF down the pipeline to `base64` and `gzip`/`tar`.
- **`version.properties`**:
  - Incremented `versionCode` from `29` to `30` and `debugCode` from `0029` to `0030`.

## 4. Commands Run and Results
- `compile_applet`: Succeeded.
- `gradle :app:testDebugUnitTest`: Executing in background.

## 5. Assumptions Made
- The target remote shell uses standard POSIX shell heredoc syntax (`cat << 'EOF' ...`), which is supported on bash, sh, zsh, dash, and busybox.

## 6. Prior Logging Gaps Found
- PRIOR LOGGING GAPS FOUND: none

## 7. Version Increment Assessment
- **Probability Score:** 95 / 100 (Critical bug fix for active shell file transfer reliability)
- **Action Taken:** Incremented `versionCode` to `30` and `debugCode` to `0030`.
