#!/bin/bash

# ==============================================================================
# Script de Inicialização - Token Ring (DS Assignment)
# Ambiente: Fedora KDE Plasma (Usa 'konsole')
# ==============================================================================

# 1. Compilar o projeto
echo "--- A compilar o código Java ---"
javac ds/assignment/tring/*.java

if [ $? -ne 0 ]; then
    echo "ERRO: A compilação falhou. Verifica o código."
    exit 1
fi
echo "Compilação terminada com sucesso."

# Definições de Rede Local
HOST="localhost"
SERVER_PORT=33333

# --- 2. Iniciar o CalculatorServer ---
echo "--- A iniciar o Calculator Server ---"

konsole --new-tab --title "Calculator Server" \
    -e java ds.assignment.tring.CalculatorServer $HOST &

sleep 2 # Espera o servidor arrancar

echo "--- A iniciar os Peers ---"

# Peer 1 (5000) -> Vizinho Peer 2 (5001)
konsole --new-tab --title "Peer 1 (Port 5000)" \
    -e java ds.assignment.tring.Peer $HOST 5000 $HOST 5001 $HOST &

# Peer 2 (5001) -> Vizinho Peer 3 (5002)
konsole --new-tab --title "Peer 2 (Port 5001)" \
    -e java ds.assignment.tring.Peer $HOST 5001 $HOST 5002 $HOST &

# Peer 3 (5002) -> Vizinho Peer 4 (5003)
konsole --new-tab --title "Peer 3 (Port 5002)" \
    -e java ds.assignment.tring.Peer $HOST 5002 $HOST 5003 $HOST &

# Peer 4 (5003) -> Vizinho Peer 5 (5004)
konsole --new-tab --title "Peer 4 (Port 5003)" \
    -e java ds.assignment.tring.Peer $HOST 5003 $HOST 5004 $HOST &

# Peer 5 (5004) -> Vizinho Peer 1 (5000) [FECHA O ANEL]
konsole --new-tab --title "Peer 5 (Port 5004)" \
    -e java ds.assignment.tring.Peer $HOST 5004 $HOST 5000 $HOST &

echo "Aguardando 5 segundos para os Peers estabilizarem..."
sleep 5

# --- 4. Injetar o Token ---
# Envia o token para o Peer 1 para começar a rodar
echo "--- A injetar o Token no anel (Peer 1) ---"
java ds.assignment.tring.Token $HOST 5000
