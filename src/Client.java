
import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Что-то пошло не так...");
                return;
            }
        }
        if(clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено.\n" +
                    "Для выхода наберите команду 'exit'.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }
        while (clientConnected) {
            String testFromConsole = ConsoleHelper.readString();
            if(testFromConsole.equals("exit")) {
                break;
            }
            if(shouldSendTextFromConsole()) {
                sendTextMessage(testFromConsole);
            }
        }
    }


    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        String serverAddress = ConsoleHelper.readString();
        return serverAddress;
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите адрес порта сервера:");
        int portServerAddress = ConsoleHelper.readInt();
        return portServerAddress;
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Представьтесь:");
        String userName = ConsoleHelper.readString();
        return userName;
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Не удалось отправить сообщение");
            clientConnected = false;
        }
    }

    public class SocketThread extends Thread {

        @Override
        public void run() {
            String adressServer = getServerAddress();
            int port = getServerPort();

            Socket socket = null;

            try {
                socket = new Socket(adressServer, port);
                Connection connection = new Connection(socket);
                Client.this.connection = connection;

                clientHandshake();
                clientMainLoop();


            } catch (IOException e) {
                e.printStackTrace( );
                notifyConnectionStatusChanged(false);
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace( );
                notifyConnectionStatusChanged(false);
            }

        }

        protected void processIncomingMessage(String message) {
            System.out.println(message);
        }
        protected void informAboutAddingNewUser(String userName) {
            System.out.println("Участник с именем " + userName + " присоединился к чату.");
        }
        protected void informAboutDeletingNewUser(String userName) {
            System.out.println("Участник с именем " + userName + " покинул чат.");
        }
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            synchronized(Client.this) {
                Client.this.clientConnected = clientConnected;
                Client.this.notify();
            }
        }


        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() != null) {

                    switch (message.getType()) {

                        // 	Если тип полученного сообщения NAME_REQUEST (сервер запросил имя)
                        case NAME_REQUEST: {

                            // запросить ввод имени пользователя с помощью метода getUserName()
                            // создать новое сообщение с типом USER_NAME и введенным именем, отправить сообщение серверу.
                            String userName = getUserName();
                            connection.send(new Message(MessageType.USER_NAME, userName));
                            break;
                        }

                        // Если тип полученного сообщения NAME_ACCEPTED (сервер принял имя)
                        case NAME_ACCEPTED: {

                            // значит сервер принял имя клиента, нужно об этом сообщить главному потоку, он этого очень ждет.
                            // Сделай это с помощью метода notifyConnectionStatusChanged(), передав в него true. После этого выйди из метода.
                            notifyConnectionStatusChanged(true);
                            return;
                        }

                        default: {
                            throw new IOException("Unexpected MessageType");
                        }
                    }
                } else {
                    throw new IOException("Unexpected MessageType");
                }

            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() != null) {

                    switch (message.getType()) {
                        case TEXT:
                            processIncomingMessage(message.getData());
                            break;
                        case USER_ADDED:
                            informAboutAddingNewUser(message.getData());
                            break;
                        case USER_REMOVED:
                            informAboutDeletingNewUser(message.getData());
                            break;
                        default:
                            throw new IOException("Unexpected MessageType");
                    }
                } else {
                    throw new IOException("Unexpected MessageType");
                }

            }
        }


    }
}
