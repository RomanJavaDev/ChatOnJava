
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


// Сервер должен поддерживать множество соединений с разными клиентами одновременно.
//Это можно реализовать с помощью следующего алгоритма:
//
//- Сервер создает серверное сокетное соединение.
//- В цикле ожидает, когда какой-то клиент подключится к сокету.
//- Создает новый поток обработчик Handler, в котором будет происходить обмен сообщениями с клиентом.
//- Ожидает следующее соединение.

public class Server {

    // Статическое поле Map<String, Connection> connectionMap, где ключом будет имя клиента, а значением - соединение с ним.
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();


    // Статический метод void sendBroadcastMessage(Message message),
    // который должен отправлять сообщение message всем соединениям из connectionMap.
    // Если при отправке сообщение произойдет исключение IOException, нужно отловить его и сообщить пользователю,
    // что не смогли отправить сообщение.
    public static void sendBroadcastMessage(Message message) {
        for (String name : connectionMap.keySet()) {
            try {
                connectionMap.get(name).send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage(String.format("Can't send the message to %s", name));
            }
        }
    }

    // Метод main класса Server, должен:
    //
    //а) Запрашивать порт сервера, используя ConsoleHelper.
    //б) Создавать серверный сокет java.net.ServerSocket, используя порт из предыдущего пункта.
    //в) Выводить сообщение, что сервер запущен.
    //г) В бесконечном цикле слушать и принимать входящие сокетные соединения только что созданного серверного сокета.
    //д) Создавать и запускать новый поток Handler, передавая в конструктор сокет из предыдущего пункта.
    //е) После создания потока обработчика Handler переходить на новый шаг цикла.
    //ж) Предусмотреть закрытие серверного сокета в случае возникновения исключения.
    //з) Если исключение Exception все же произошло, поймать его и вывести сообщение об ошибке.
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


    // Класс Handler должен реализовывать протокол общения с клиентом.
    //Выделим из протокола отдельные этапы и реализуем их с помощью отдельных методов
    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }
        @Override
        public void run() {
            // Выводится сообщение, что установлено новое соединение с удаленным адресом,
            // который можно получить с помощью метода getRemoteSocketAddress().
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


        // Этап первый - это этап рукопожатия (знакомства сервера с клиентом).
        //Реализуем его с помощью приватного метода String serverHandshake(Connection connection) throws IOException, ClassNotFoundException.
        //Метод в качестве параметра принимает соединение connection, а возвращает имя нового клиента.
        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true){ // Если какая-то проверка не прошла, заново запросить имя клиента
                connection.send(new Message(MessageType.NAME_REQUEST)); // Сформировать и отправить команду запроса имени пользователя
                Message answer = connection.receive(); // Получить ответ клиента
                if (answer.getType() != MessageType.USER_NAME) continue; // Проверить, что получена команда с именем пользователя
                String userName = answer.getData(); // Достать из ответа имя,
                if (userName == null || userName.isEmpty()) continue; // проверить, что оно не пустое
                if (connectionMap.containsKey(userName)) continue; // и пользователь с таким именем еще не подключен (используй connectionMap)
                connectionMap.put(userName, connection); //  Добавить нового пользователя и соединение с ним в connectionMap
                connection.send(new Message(MessageType.NAME_ACCEPTED)); // Отправить клиенту команду информирующую, что его имя принято
                return userName; // Вернуть принятое имя в качестве возвращаемого значения
            }
        }

        // отправка клиенту (новому участнику) информации об остальных клиентах (участниках) чата
        // connection - соединение с участником, которому будем слать информацию, а userName - его имя.
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
                Message message = connection.receive(); //  Принимает сообщение клиента
                // Если принятое сообщение - это текст (тип TEXT),
                // то формировать новое текстовое сообщение путем конкатенации: имени клиента, двоеточия, пробела и текста сообщения.
                // Отправлять сформированное сообщение всем клиентам с помощью метода sendBroadcastMessage().
                if (message != null && message.getType() == MessageType.TEXT) {
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                } else {
                    ConsoleHelper.writeMessage("Error!");
                }
            }

        }

    }
}
