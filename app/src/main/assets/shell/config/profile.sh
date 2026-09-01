# BinBox Shell Profile
export TERM="${TERM:-xterm-256color}"
export COLORTERM="${COLORTERM:-truecolor}"
export LANG="${LANG:-en_US.UTF-8}"
export PS1='\[\033[1;32m\]\u@binbox\[\033[0m\]:\[\033[1;34m\]\w\[\033[0m\]\$ '
alias ll='ls -la'
alias l='ls -CF'
alias la='ls -A'
