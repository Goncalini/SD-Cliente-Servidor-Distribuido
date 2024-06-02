package connection;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TaggedConnection {

    public static class Frame {
        private final int tag;
        private final byte[] data;
        private final String fileName;
        private final String userName;
        private String password;
        private final int memoriaProg;

        public Frame(int tag, String username, String filename, int memoriaProg, byte[] data){
            this.tag = tag;
            this.data = data;
            this.fileName = filename;
            this.userName = username;
            this.password = "";
            this.memoriaProg = memoriaProg;
        }

        public Frame(int tag, String username, String password){
            this.tag = tag;
            this.data = new byte[0];
            this.fileName = "";
            this.userName = username;
            this.password = password;
            this.memoriaProg = 0;
        }

        public Frame(int tag, byte[] data) {
            this.tag = tag;
            this.data = data;
            this.fileName = "";
            this.userName = "";
            this.password = "";
            this.memoriaProg = 0;
        }

        public int getTag() {
            return this.tag;
        }

        public byte[] getData() {
            return this.data;
        }

        public String getFileName() {
            return fileName;
        }

        public String getUserName() {
            return userName;
        }

        public String getPassword() {
            return password;
        }

        public int getMemoriaProg() {
            return memoriaProg;
        }
    }

    private final Socket socket;
    private final ReentrantLock readLock;
    private final ReentrantLock writeLock;
    private final DataInputStream in;
    private final DataOutputStream out;

    public TaggedConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
        this.in = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        this.readLock = new ReentrantLock();
        this.writeLock = new ReentrantLock();
    }

    public void send(Frame frame) throws IOException {
        try {
            this.writeLock.lock();
            this.out.writeInt(frame.tag);
            this.out.writeInt(frame.data.length);
            this.out.write(frame.data);
            this.out.flush();
        } finally {
            this.writeLock.unlock();
        }
    }

    public void send(int tag,byte[] data) throws IOException {
        try {
            this.writeLock.lock();
            this.out.writeInt(tag);
            this.out.writeInt(data.length);
            this.out.write(data);
            this.out.flush();
        } finally {
            this.writeLock.unlock();
        }
    }

    public void send(int tag, boolean bool) throws IOException {
        try {
            this.writeLock.lock();
            this.out.writeInt(tag);
            this.out.writeBoolean(bool);
            this.out.flush();
        } finally {
            this.writeLock.unlock();
        }
    }

    public void send(String username, String password,int tag) throws IOException{
        try {
            this.writeLock.lock();
            this.out.writeInt(tag);
            this.out.writeUTF(username);
            this.out.writeUTF(password);
            this.out.flush();
        } finally {
            this.writeLock.unlock();
        }
    }

    public void send(String username, String filename, int memoriaProg,byte[] data, int tag) throws IOException {
        try {
            this.writeLock.lock();
            this.out.writeInt(tag);
            this.out.writeUTF(username);
            this.out.writeUTF(filename);
            this.out.writeInt(memoriaProg);
            this.out.writeInt(data.length);
            this.out.write(data);
            this.out.flush();
        } finally {
            this.writeLock.unlock();
        }
    }

    public Frame receive2() throws IOException {
        try {
            this.readLock.lock();
            int tag = this.in.readInt();
            int length = this.in.readInt();
            byte[] bytes = new byte[length];
            this.in.readFully(bytes);
            return new Frame(tag,bytes);
        } finally {
            this.readLock.unlock();
        }
    }

    public Frame receive() throws IOException {
        this.readLock.lock();
        try {
            int tag = this.in.readInt();
            switch (tag){
                case(1), (2) -> {
                    String username = this.in.readUTF();
                    String password = this.in.readUTF();
                    return new Frame(tag,username,password);
                }
                case(3), (4) -> {
                    int length = this.in.readInt();
                    byte[] bytes = new byte[length];
                    this.in.readFully(bytes);
                    return new Frame(tag,bytes);
                }
                case(5) -> {
                    String username = this.in.readUTF();
                    String nomeProg = this.in.readUTF();
                    int memoriaProg = this.in.readInt();
                    int length = this.in.readInt();
                    byte[] bytes = new byte[length];
                    this.in.readFully(bytes);
                    return new Frame(tag,username,nomeProg,memoriaProg,bytes);
                }
            }
        } finally {
            this.readLock.unlock();
        }
        return null;
    }


    public void close() throws IOException {
        boolean writeLockAcquired = false;
        boolean readLockAcquired = false;

        try {
            writeLockAcquired = this.writeLock.tryLock();
            if (writeLockAcquired) {
                readLockAcquired = this.readLock.tryLock();
                if (readLockAcquired) {
                    this.socket.close();
                }
            } else {
                throw new IOException("Unable to close socket: write lock not acquired");
            }
        } catch (Exception ignored) {

        } finally {
            if (readLockAcquired) {
                this.readLock.unlock();
            }
            if (writeLockAcquired) {
                this.writeLock.unlock();
            }
        }
    }

}

