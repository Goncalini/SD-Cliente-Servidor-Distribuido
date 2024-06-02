import connection.Demultiplexer;
import connection.TaggedConnection;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client {

    private Socket socket;
    private String username;
    private Scanner scan;

    public Client(Scanner scan) throws IOException{
        this.socket = new Socket("localhost",12345);
        this.username = null;
        this.scan = scan;
    }

    // Menu do user
    public void menuUser(Demultiplexer demul) {

        Menu menu = new Menu(new String[]{"Fazer Pedido", "Consultar Estado da memória", "Consultar Pedidos Pendentes", "Logout"});

        do {
            menu.executa();

            switch(menu.getOpcao()){
                case(1) -> {
                    try {
                        enviaFile(menu, demul);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                case(2) -> {
                    try {
                        consultarEstado(demul);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                case(3) -> {
                    try {
                        consultarPedidos(demul);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                case(4) -> {
                    this.username = null;
                }
            }
        } while (menu.getOpcao() != 4);
    }

    // Menu normal
    public void menuRun(Demultiplexer demul) throws IOException {

        Menu menu = new Menu(new String[]{"Registrar Usuário", "Autenticar Usuário", "Sair"});

        do {
            menu.executa();
            switch (menu.getOpcao()) {
                case(1) -> {
                    try {
                        registerUser(menu, demul);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                case (2) -> {
                    try {
                        authenticateUser(menu, demul);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } while (menu.getOpcao() != 3);
        this.socket.close();
    }

    private void registerUser(Menu menu, Demultiplexer demul) throws IOException {

        System.out.print("Nome de usuário: ");
        String username = menu.getScanner().nextLine();
        System.out.print("Password: ");
        String password = menu.getScanner().nextLine();

        demul.send(1,username,password);

        try {
            byte[] bla = demul.receive(1);
            System.out.println(new String(bla));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void authenticateUser(Menu menu, Demultiplexer demul) throws IOException {

        System.out.print("Nome de usuário: ");
        String username = menu.getScanner().nextLine();
        System.out.print("Password: ");
        String password = menu.getScanner().nextLine();

        demul.send(2,username,password);

        try {
            byte[] bla = demul.receive(2);
            ByteArrayInputStream bytes2 = new ByteArrayInputStream(bla);
            DataInputStream stream2 = new DataInputStream(bytes2);
            boolean autenticado = stream2.readBoolean();
            if (autenticado) {
                System.out.println("Autenticado com sucesso : " + username);
                this.username = username;
                menuUser(demul);
            } else {
                System.out.println("Falha na autenticação : " + username);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void consultarEstado(Demultiplexer demul) throws IOException {
        demul.send(new TaggedConnection.Frame(3, new byte[0]));
        try {
            byte[] estado = demul.receive(3);
            System.out.println("Estado Atual:\n" + new String(estado));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void consultarPedidos(Demultiplexer demul) throws IOException{
        demul.send(new TaggedConnection.Frame(4,new byte[0]));
        try {
            byte[] estado = demul.receive(4);
            System.out.println("Pedidos :\n" + new String(estado));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

     // versão linux
    private void enviaFile(Menu menu, Demultiplexer demul) throws IOException {
        System.out.print("Nome do ficheiro: ");
        String file = menu.getScanner().nextLine();
        System.out.print("Memória necessária: ");
        int memoriaprog = menu.getScanner().nextInt();

        File fileExecute = new File(file);
        byte[] array = new byte[(int) fileExecute.length()];

        if(array.length > 0) {
            Thread thread = new Thread(() -> {
                try {
                    demul.send(5, username, file, memoriaprog, array);
                    byte[] bla = demul.receive(5);

                    String outputFileName = username + "_" + file;

                    String resultFileName = "Resultados/" + outputFileName;
                    File resultFile = new File(resultFileName);
                    FileOutputStream outputStream = new FileOutputStream(resultFile);
                    outputStream.write(bla);
                    outputStream.close();

                    System.out.println("\nTarefa executada com sucesso!");

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }
        else System.out.println("Ficheiro não existe!");

    }


     /* // versão windows
     private void enviaFile(Menu menu, Demultiplexer demul) throws IOException {
         System.out.print("Nome do ficheiro: ");
         String file = menu.getScanner().nextLine();
         System.out.print("Memória necessária: ");
         int memoriaprog = menu.getScanner().nextInt();

         File fileExecute = new File(file);
         byte[] array = new byte[(int) fileExecute.length()];

         if (array.length > 0) {
             String outputFileName = file.replace("|", "_"); // Substituir "|" por "_"

             Thread thread = new Thread(() -> {
                 try {
                     demul.send(5, username, file, memoriaprog, array);
                     byte[] bla = demul.receive(5);
                     System.out.println("2Tarefa executada com sucesso!");

                     String resultFileName = "Resultados/" + outputFileName;
                     File resultFile = new File(resultFileName);
                     FileOutputStream outputStream = new FileOutputStream(resultFile);
                     outputStream.write(bla);
                     System.out.println("1Tarefa executada com sucesso!");
                     outputStream.close();

                     System.out.println("Tarefa executada com sucesso!");

                 } catch (IOException | InterruptedException e) {
                     e.printStackTrace();
                 }
             });
             thread.start();
         } else {
             System.out.println("Ficheiro não existe!");
         }
     }

    */

    public void run(){
        try (Demultiplexer dem = new Demultiplexer(new TaggedConnection(this.socket))) {
            dem.start();
            this.menuRun(dem);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}


