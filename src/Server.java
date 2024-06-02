import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Server {

    private static int memoria_max = 1024;
    private static int memoria_atual = 0;

    private static ArrayDeque<String> pedidos;

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final ReentrantLock workerLock = new ReentrantLock();
    private static final Condition cond = workerLock.newCondition();
    private ServerSocket serverSocket;
    private GestaoUtilizador utilizadores;
    private static final int PORTA = 12345;

    public Server() throws IOException{
        this.serverSocket = new ServerSocket(PORTA);
        this.utilizadores = new GestaoUtilizador();
        pedidos = new ArrayDeque<>();
    }

    public static int getMemoria_max() {
        try {
            lock.readLock().lock();
            return memoria_max;
        } finally {
            lock.readLock().unlock();
        }
    }

    public static int getMemoria_atual() {
        try {
            lock.readLock().lock();
            return memoria_atual;
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void setMemoria_atual(int nova_memoria_atual) {
        try {
            lock.writeLock().lock();
            memoria_atual = nova_memoria_atual;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static ArrayDeque<String> getPedidos() {
        try {
            lock.readLock().lock();
            return new ArrayDeque<>(pedidos);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void addPedido(String pedido) {
        try {
            lock.writeLock().lock();
            if (pedidos == null) {
                pedidos = new ArrayDeque<>();
            }
            Server.pedidos.add(pedido);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void retiraPedido() {
        try {
            lock.writeLock().lock();
            if (pedidos == null) {
                pedidos = new ArrayDeque<>();
            }
            Server.pedidos.poll();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void run() throws IOException{
        try{
            while(true){
                Socket clientSocket = this.serverSocket.accept();
                System.out.println("Nova conexão: " + clientSocket);

                new Thread(new ServerWorker(clientSocket,this.utilizadores,workerLock,cond)).start();
            }
        } finally{
            this.serverSocket.close();
        }
    }
}

