
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message) {
        for (String name : connectionMap.keySet()) {
            try {
                connectionMap.get(name).send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage(String.format("Can't send the message to %s", name));
            }
        }
    }

    public static void main(String[] args) throws IOException {

        try (ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())) {
            System.out.println("Server has started");
            while (true) {
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }
        @Override
        public void run() {
//            super.run();
            ConsoleHelper.writeMessage("Connection established with address: " + socket.getRemoteSocketAddress());
            String userName = null;

            try (Connection connection = new Connection(socket)){
                //Вызывать метод, реализующий рукопожатие с клиентом, сохраняя имя нового клиента.
                userName = serverHandshake(connection);

                // Рассылать всем участникам чата информацию об имени присоединившегося участника
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));

                // Сообщать новому участнику о существующих участниках.
                notifyUsers(connection, userName);

                //Запускать главный цикл обработки сообщений сервером
                serverMainLoop(connection, userName);

            } catch (ClassNotFoundException | IOException e) {
                ConsoleHelper.writeMessage("В Handler произошла ошибка при обмене данными с удаленным адресом");
//                e.printStackTrace();
            } finally {

                //После того как все исключения обработаны, если п.11.3 отработал и возвратил нам имя,
                //мы должны удалить запись для этого имени из connectionMap и разослать всем остальным участникам сообщение
                //с типом USER_REMOVED и сохраненным именем.
                if (userName != null) {
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                    // ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто");
                }

            }
            ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто");
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true){
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message answer = connection.receive();
                if (answer.getType() != MessageType.USER_NAME) continue;
                String userName = answer.getData();
                if (userName == null || userName.isEmpty()) continue;
                if (connectionMap.containsKey(userName)) continue;
                connectionMap.put(userName, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return userName;
            }
        }
        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (String name : connectionMap.keySet()) {
                if (!name.equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, name));
//                    connectionMap.get(name).send(new Message(MessageType.USER_ADDED, "Добавлен новый пользователь с именем " + userName));
                }
            }

        }
        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message != null && message.getType() == MessageType.TEXT) {
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                } else {
                    ConsoleHelper.writeMessage("Error!");
                }
            }

        }

    }
}
