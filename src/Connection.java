
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketAddress;

// Класс Connection будет выполнять роль обертки над классом java.net.Socket,
//которая должна будет уметь сериализовать и десериализовать объекты типа Message в сокет.

public class Connection implements Closeable {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());

    }

    // Метод void send(Message message) throws IOException.
    //Он должен записывать (сериализовать) сообщение message в ObjectOutputStream.
    //Этот метод будет вызываться из нескольких потоков.
    // Запись в объект ObjectOutputStream возможна только одним потоком в определенный момент времени, благодаря synchronized
    //остальные желающие ждут завершения записи.
    //При этом другие методы класса Connection не должны быть заблокированы.
    public void send(Message message) throws IOException {
        synchronized(out) {
            out.writeObject(message);
            out.flush();
        }

    }

    // Метод Message receive() throws IOException, ClassNotFoundException.
    //Он должен читать (десериализовать) данные из ObjectInputStream.
    //Операция чтения не может быть одновременно вызвана несколькими потоками,
    //при этом вызовы других методов класса Connection не блокируются.
    public Message receive() throws IOException, ClassNotFoundException {
        synchronized(in) {
            return (Message) in.readObject();
        }

    }

    // Метод SocketAddress getRemoteSocketAddress() возвращает удаленный адрес сокетного соединения.
    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();

    }
    @Override
    public void close() throws IOException {
        if (socket != null) socket.close();
        if (out != null) out.close();
        if (in != null) in.close();

    }
}
