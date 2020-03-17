
import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;


    // метод public static void main(String[] args).
    //Он должен создавать новый объект типа Client и вызывать у него метод run().
    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    // Метод void run() должен создавать вспомогательный поток SocketThread,
    // ожидать пока тот установит соединение с сервером, а после этого в цикле считывать сообщения с консоли и отправлять их серверу.
    //Условием выхода из цикла будет отключение клиента или ввод пользователем команды 'exit'.
    //Для информирования главного потока, что соединение установлено во вспомогательном потоке,
    // используй методы wait() и notify() объекта класса Client.
    public void run() {
        SocketThread socketThread = getSocketThread(); // Создается новый сокетный поток
        //  Помечать созданный поток как daemon, это нужно для того,
        //  чтобы при выходе из программы вспомогательный поток прервался автоматически.
        socketThread.setDaemon(true);
        socketThread.start(); // Запустить вспомогательный поток.
        synchronized (this) {
            try {
                wait(); // Заставить текущий поток ожидать, пока он не получит нотификацию из другого потока.
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Что-то пошло не так...");
                return;
            }
        }
        // После того, как поток дождался нотификации, проверь значение clientConnected.
        //Если оно true - выведи "Соединение установлено.
        //Для выхода наберите команду 'exit'.".
        //Если оно false - выведи "Произошла ошибка во время работы клиента.".
        if(clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено.\n" +
                    "Для выхода наберите команду 'exit'.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }
        // Считывай сообщения с консоли пока клиент подключен.
        //Если будет введена команда 'exit', то выйди из цикла.
        while (clientConnected) {
            String testFromConsole = ConsoleHelper.readString();
            if(testFromConsole.equals("exit")) {
                break;
            }

            // После каждого считывания, если метод shouldSendTextFromConsole() возвращает true,
            // отправь считанный текст с помощью метода sendTextMessage().
            if(shouldSendTextFromConsole()) {
                sendTextMessage(testFromConsole);
            }
        }
    }

    // Метод должен запросить ввод адреса сервера у пользователя и вернуть введенное значение.
    // Адрес может быть строкой, содержащей ip, если клиент и сервер запущен на разных машинах
    // или 'localhost', если клиент и сервер работают на одной машине.
    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        String serverAddress = ConsoleHelper.readString();
        return serverAddress;
    }

    // Метод должен запрашивать ввод порта сервера и возвращать его.
    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите адрес порта сервера:");
        int portServerAddress = ConsoleHelper.readInt();
        return portServerAddress;
    }

    // Метод должен запрашивать и возвращать имя пользователя.
    protected String getUserName() {
        ConsoleHelper.writeMessage("Представьтесь:");
        String userName = ConsoleHelper.readString();
        return userName;
    }

    // Метод в данной реализации клиента всегда должен возвращать true (мы всегда отправляем текст введенный в консоль).
    //Этот метод может быть переопределен, если мы будем писать какой-нибудь другой клиент,
    // унаследованный от нашего, который не должен отправлять введенный в консоль текст.
    protected boolean shouldSendTextFromConsole() {
        return true;
    }


    // Метод должен создавать и возвращать новый объект класса SocketThread.
    protected SocketThread getSocketThread() {
        return new SocketThread();
    }


    // Метод void sendTextMessage(String text) создает новое текстовое сообщение, используя переданный текст и отправляет
    // его серверу через соединение connection.
    //Если во время отправки произошло исключение IOException, то необходимо вывести информацию об этом пользователю
    // и присвоить false полю clientConnected.
    protected void sendTextMessage(String text) {
        try {
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Не удалось отправить сообщение");
            clientConnected = false;
        }
    }

    // Класс SocketThread отвечает за поток, устанавливающий сокетное соединение и читающий сообщения сервера.
    public class SocketThread extends Thread {

        @Override
        public void run() {
            // Запрашиваем адрес и порт сервера с помощью методов
            String adressServer = getServerAddress();
            int port = getServerPort();

            Socket socket = null; // Создаем новый объект класса java.net.Socket

            try {
                socket = new Socket(adressServer, port);
                Connection connection = new Connection(socket);  // Создаем объект класса Connection
                Client.this.connection = connection;

                clientHandshake(); // Вызываем метод, реализующий "рукопожатие" клиента с сервером
                clientMainLoop(); // Вызываем метод, реализующий основной цикл обработки сообщений сервера.


            } catch (IOException e) {
                e.printStackTrace( );
                notifyConnectionStatusChanged(false);
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace( );
                notifyConnectionStatusChanged(false);
            }

        }

        // Метод должен выводить текст message в консоль.
        protected void processIncomingMessage(String message) {
            System.out.println(message);
        }

        //  Метод должен выводить в консоль информацию о том, что участник с именем userName присоединился к чату.
        protected void informAboutAddingNewUser(String userName) {
            System.out.println("Участник с именем " + userName + " присоединился к чату.");
        }

        // Метод должен выводить в консоль, что участник с именем userName покинул чат.
        protected void informAboutDeletingNewUser(String userName) {
            System.out.println("Участник с именем " + userName + " покинул чат.");
        }

        // Метод должен:
        //а) Устанавливать значение поля clientConnected внешнего объекта Client в соответствии с переданным параметром.
        //б) Оповещать (пробуждать ожидающий) основной поток класса Client.
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            synchronized(Client.this) {
                Client.this.clientConnected = clientConnected;
                Client.this.notify();
            }
        }


        // Этот метод будет представлять клиента серверу.
        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive(); // В цикле получать сообщения, используя соединение connection.
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
