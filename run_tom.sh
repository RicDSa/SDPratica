#!/bin/bash

# ==========================================
# Script de Execução para TOM - 6 PEERS
# ==========================================

# 1. Limpeza e Compilação
echo "[INFO] A limpar e a compilar..."
rm -rf bin
mkdir bin

# Compila todos os ficheiros java nos pacotes p2p e tom
javac -d bin src/ds/assignment/tom/*.java src/ds/assignment/p2p/*.java

if [ $? -ne 0 ]; then
    echo "[ERRO] A compilação falhou. Verifica o código."
    exit 1
fi

echo "[INFO] Compilação concluída com sucesso."

# 2. Definição dos Peers
HOST="localhost"
PORT1=11111
PORT2=22222
PORT3=33333
PORT4=44444
PORT5=55555
PORT6=66666

# Definição dos Argumentos (Topologia Totalmente Conectada)
# Formato: <host_local> <port_local> [<vizinho_host> <vizinho_port> ...]
# Cada peer recebe a lista de TODOS os outros, exceto ele próprio.

ARGS_P1="$HOST $PORT1 $HOST $PORT2 $HOST $PORT3 $HOST $PORT4 $HOST $PORT5 $HOST $PORT6"
ARGS_P2="$HOST $PORT2 $HOST $PORT1 $HOST $PORT3 $HOST $PORT4 $HOST $PORT5 $HOST $PORT6"
ARGS_P3="$HOST $PORT3 $HOST $PORT1 $HOST $PORT2 $HOST $PORT4 $HOST $PORT5 $HOST $PORT6"
ARGS_P4="$HOST $PORT4 $HOST $PORT1 $HOST $PORT2 $HOST $PORT3 $HOST $PORT5 $HOST $PORT6"
ARGS_P5="$HOST $PORT5 $HOST $PORT1 $HOST $PORT2 $HOST $PORT3 $HOST $PORT4 $HOST $PORT6"
ARGS_P6="$HOST $PORT6 $HOST $PORT1 $HOST $PORT2 $HOST $PORT3 $HOST $PORT4 $HOST $PORT5"

# 3. Definição do Terminal (Deteta Konsole ou usa alternativa)
if command -v konsole &> /dev/null; then
    # KDE Konsole (Adiciona abas ou janelas)
    TERM_CMD="konsole --hold -e"
elif command -v gnome-terminal &> /dev/null; then
    TERM_CMD="gnome-terminal --"
else
    TERM_CMD="xterm -hold -e"
fi

echo "[INFO] A iniciar 6 Peers em janelas separadas..."
echo "[AVISO] Tens 10 segundos para organizar as janelas antes do chat começar!"

# 4. Execução dos 6 Peers em paralelo
$TERM_CMD java -cp bin ds.assignment.tom.Peer $ARGS_P1 &
$TERM_CMD java -cp bin ds.assignment.tom.Peer $ARGS_P2 &
$TERM_CMD java -cp bin ds.assignment.tom.Peer $ARGS_P3 &
$TERM_CMD java -cp bin ds.assignment.tom.Peer $ARGS_P4 &
$TERM_CMD java -cp bin ds.assignment.tom.Peer $ARGS_P5 &
$TERM_CMD java -cp bin ds.assignment.tom.Peer $ARGS_P6 &

echo "[INFO] Todos os processos iniciados."