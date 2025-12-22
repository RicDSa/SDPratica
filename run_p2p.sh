#!/bin/bash


# 1. Compilar o código
javac ds/assignment/p2p/*.java

if [ $? -ne 0 ]; then
    echo "Erro na compilação. Corrija os erros antes de continuar."
    exit 1
fi

echo "Código compilado com sucesso."

# Definição das Portas
P1=10001
P2=10002
P3=10003
P4=10004
P5=10005
P6=10006

HOST="localhost"
MAIN_CLASS="ds.assignment.p2p.Peer"

# Função auxiliar para abrir nova aba no Konsole
# Argumentos: $1=Titulo $2=Comando
launch_peer() {
    # --new-tab: Cria nova aba
    # -p tabtitle="...": Define o nome da aba
    # -e: Executa o comando
    konsole --new-tab -p tabtitle="$1" -e bash -c "$2; echo 'Processo terminou.'; read -p 'Pressione Enter para sair...'" &
    
    # Pequena pausa para garantir que o Konsole processa a ordem de abertura das abas
    sleep 0.5
}

# --- LANÇAMENTO DOS PEERS ---

# PEER 1 (Valor Inicial: 1.0)
CMD_P1="java $MAIN_CLASS $HOST $P1 $HOST $P2 1.0"
launch_peer "Peer 1 (Val: 1.0)" "$CMD_P1"

# PEER 2 (Valor Inicial: 0.0) -> Nó central
CMD_P2="java $MAIN_CLASS $HOST $P2 $HOST $P1 $HOST $P3 $HOST $P4 0.0"
launch_peer "Peer 2 (Val: 0.0)" "$CMD_P2"

# PEER 3 (Valor Inicial: 0.0)
CMD_P3="java $MAIN_CLASS $HOST $P3 $HOST $P2 0.0"
launch_peer "Peer 3 (Val: 0.0)" "$CMD_P3"

# PEER 4 (Valor Inicial: 0.0) -> Outro nó central
CMD_P4="java $MAIN_CLASS $HOST $P4 $HOST $P2 $HOST $P5 $HOST $P6 0.0"
launch_peer "Peer 4 (Val: 0.0)" "$CMD_P4"

# PEER 5 (Valor Inicial: 0.0)
CMD_P5="java $MAIN_CLASS $HOST $P5 $HOST $P4 0.0"
launch_peer "Peer 5 (Val: 0.0)" "$CMD_P5"

# PEER 6 (Valor Inicial: 0.0)
CMD_P6="java $MAIN_CLASS $HOST $P6 $HOST $P4 0.0"
launch_peer "Peer 6 (Val: 0.0)" "$CMD_P6"