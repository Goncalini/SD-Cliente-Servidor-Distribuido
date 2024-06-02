package connection;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Demultiplexer implements AutoCloseable{

    private final TaggedConnection tagConn;
    private final ReentrantLock lock;
    private final Map<Integer, Message> buf;
    private Thread thread;


    private class Message {
        int waiters;
        final Condition cond;
        final ArrayDeque <byte[]> queue;

        public Message(){
            this.waiters = 0;
            this.cond = lock.newCondition();
            this.queue = new ArrayDeque<>();
        }
    }

    private Message getMessage(int tag){
        Message e = buf.get(tag);
        if (e == null){
            e = new Message();
            buf.put(tag,e);
        }
        return e;
    }

    public Demultiplexer(TaggedConnection tagConn) {
        this.tagConn = tagConn;
        this.lock = new ReentrantLock();
        this.buf = new HashMap<>();
    }

    public void start() {
        this.thread = new Thread(() -> {
            try  {
                while(true){

                    TaggedConnection.Frame frame = tagConn.receive2();
                    lock.lock();
                    try{
                        int tag = frame.getTag();
                        byte[] data = frame.getData();
                        Message e = getMessage(tag);
                        e.queue.add(data);
                        e.cond.signal();
                    }
                    finally {
                        lock.unlock();
                    }
                }
            }  catch (Exception ignored) {}
        });
        this.thread.start();
    }

    public void send(TaggedConnection.Frame frame) throws IOException {
        tagConn.send(frame);
    }

    public void send(int tag, String username, String password) throws IOException {
        tagConn.send(username,password,tag);
    }

    public void send(int tag, byte[] data) throws IOException {
        tagConn.send(tag,data);
    }

    public void send(int tag,String username, String filename, int memoriaProg,byte[] data) throws IOException {
        tagConn.send(username, filename, memoriaProg,data,tag);
    }

        public byte[] receive(int tag) throws IOException, InterruptedException {
        lock.lock();
        try {
            Message e = getMessage(tag);
            e.waiters++;
            while(true){
                if (!e.queue.isEmpty()) {
                    byte[] res = e.queue.poll();
                    e.waiters--;
                    if (e.queue.isEmpty() && e.waiters == 0)
                        buf.remove(tag);
                    return res;
                }
                e.cond.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public void close() throws IOException {
        tagConn.close();
    }
}