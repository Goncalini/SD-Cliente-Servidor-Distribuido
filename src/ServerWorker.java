import connection.TaggedConnection;
import exceptions.JobFunctionException;
import exceptions.NomeExistenteException;

import java.io.*;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerWorker implements Runnable{

    private GestaoUtilizador utilizadores;
    private TaggedConnection conn;
    private String nome;

    private final Lock lock;
    private final Condition condition;

    public ServerWorker(Socket clientSocket, GestaoUtilizador utilizadores, ReentrantLock lock, Condition cond) throws IOException {
        this.utilizadores = utilizadores;
        this.nome = null;
        this.lock = lock;
        this.condition = cond;
        this.conn = new TaggedConnection(clientSocket);
    }

    public void run() {

        try {
            while (true) {
                TaggedConnection.Frame data = this.conn.receive();
                switch (data.getTag()) {
                    case (1) -> {
                        register(data);
                    }
                    case (2) -> {
                        authenticate(data);
                    }
                    case (3) -> {
                        consultarEstado(data);
                    }
                    case (4) -> {
                        consultarPedidos(data);
                    }
                    case (5) -> {
                        Thread thread = new Thread(() -> {
                            try {
                                jobExecute(data);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        thread.start();
                    }
                }
            }
            } catch(IOException e){
                //throw new RuntimeException(e);
            }

    }

    private void register(TaggedConnection.Frame data) throws IOException {
        try {
            String nome = data.getUserName();
            String password = data.getPassword();

            nome = this.utilizadores.verificaNome(nome);
            this.utilizadores.adicionaUtilizador(nome,password);
            this.conn.send(new TaggedConnection.Frame(data.getTag(), "Registado com sucesso".getBytes()));

        } catch (NomeExistenteException e) {
            this.conn.send(new TaggedConnection.Frame(data.getTag(), "Registo Falhou, o nome já existe!".getBytes()));
        }
    }

    private void authenticate(TaggedConnection.Frame data) throws IOException {
        try {
            String nome = data.getUserName();
            String password = data.getPassword();
            boolean bool = this.utilizadores.getUtilizadorNomePasse(nome,password);

            ByteArrayOutputStream bytes2 = new ByteArrayOutputStream();
            DataOutputStream stream2 = new DataOutputStream(bytes2);
            stream2.writeBoolean(bool);
            stream2.flush();

            this.conn.send(data.getTag(), bytes2.toByteArray());
            if (bool) {
                System.out.println("Usuário autenticado: " + nome);
                this.nome = nome;
            }
            else System.out.println("Erro de autenticação");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int obterMemoriaDisponivel() {
        return Server.getMemoria_max() - Server.getMemoria_atual();
    }

    private void consultarEstado(TaggedConnection.Frame data) throws IOException {
        String estado = "Memória Disponível: " + obterMemoriaDisponivel();
        conn.send(new TaggedConnection.Frame(data.getTag(), estado.getBytes()));
    }

      public String convertWithIteration(ArrayDeque<String> map) {
        if(map.isEmpty()) return "Não há tarefas pendentes!";
        StringBuilder dequeAsString = new StringBuilder("{");
        for (String key : map) {
            dequeAsString.append(key + ", ");
        }
        dequeAsString.delete(dequeAsString.length()-2, dequeAsString.length()).append("}");
        return dequeAsString.toString();
    }

    private void consultarPedidos(TaggedConnection.Frame data) throws IOException{
        String map = convertWithIteration(Server.getPedidos());
        conn.send(new TaggedConnection.Frame(data.getTag(), map.getBytes()));
    }

    private void liberarMemoria(int memoriaprog) {
        lock.lock();
        try {
            Server.setMemoria_atual(Server.getMemoria_atual() - memoriaprog);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void jobExecute(TaggedConnection.Frame data) throws IOException{
            try{
                String username = data.getUserName();
                String nomeProg = data.getFileName();
                int memoriaprog = data.getMemoriaProg();
                byte[] file = data.getData();
                int length = file.length;

                if (memoriaprog > Server.getMemoria_max()) {
                    String errorMsg = "Erro! Memória requerida excede a capacidade máxima do servidor. Memória requerida: " +
                            memoriaprog + ", Memória máxima do servidor: " + Server.getMemoria_max();
                    // Imprime diretamente no terminal do cliente
                    System.out.println(errorMsg);
                    return;  // Ignora o pedido
                }

                if (Server.getMemoria_atual() + memoriaprog <= Server.getMemoria_max()) {
                    Server.setMemoria_atual(Server.getMemoria_atual() + memoriaprog);

                    if(length > 0) {
                        JobFunction bla = new JobFunction();
                        String outputFileName = username + "_" + nomeProg;
                        Server.addPedido(outputFileName);
                        byte[] result = bla.execute(file);
                        Server.retiraPedido();
                        try {

                            this.conn.send(new TaggedConnection.Frame(data.getTag(),result));
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else this.conn.send(new TaggedConnection.Frame(data.getTag(), "Erro na execução!!".getBytes()));
                }

                else {
                    System.out.println("Erro! Memória insuficiente!");
                    lock.lock();
                    try {
                        String outputFileName = username + "_" + nomeProg;
                        Server.addPedido(outputFileName);
                        while(Server.getMemoria_atual() + memoriaprog > Server.getMemoria_max()) {
                            condition.await(); // espera até haver memória disponível
                        }
                        Server.setMemoria_atual(Server.getMemoria_atual() + memoriaprog);
                        JobFunction bla = new JobFunction();
                        byte[] result = bla.execute(file);
                        Server.retiraPedido();
                        try {
                            this.conn.send(new TaggedConnection.Frame(data.getTag(),result));
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        lock.unlock();
                    }
                }

                liberarMemoria(memoriaprog);

            } catch (JobFunctionException | IOException e ) {
                throw new RuntimeException(e);
            }
    }

}
