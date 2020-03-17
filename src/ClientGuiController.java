
public class ClientGuiController extends Client {
    public static void main(String[] args) {
        new ClientGuiController().run();
    }

    private ClientGuiModel model = new ClientGuiModel();
    private ClientGuiView view = new ClientGuiView(this);


    public ClientGuiModel getModel(){
        return model;
    }

    // должен получать объект SocketThread через метод getSocketThread() и вызывать у него метод run().
    @Override
    public void run() {
        getSocketThread().run();
    }

    @Override
    protected String getServerAddress() {
        return view.getServerAddress();
    }

    @Override
    protected int getServerPort() {
        return view.getServerPort();
    }

    @Override
    protected String getUserName() {
        return view.getUserName();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new GuiSocketThread();
    }


    public  class GuiSocketThread extends SocketThread{

        // должен устанавливать новое сообщение у модели и вызывать обновление вывода сообщений у представления.
        @Override
        protected void processIncomingMessage(String message) {
            model.setNewMessage(message);
            view.refreshMessages();
        }

        // должен добавлять нового пользователя в модель и вызывать обновление вывода пользователей у отображения.
        @Override
        protected void informAboutAddingNewUser(String userName) {
            model.addUser(userName);
            view.refreshUsers();
        }

        // должен удалять пользователя из модели и вызывать обновление вывода пользователей у отображения.
        @Override
        protected void informAboutDeletingNewUser(String userName) {
            model.deleteUser(userName);
            view.refreshUsers();
        }

        // должен вызывать аналогичный метод у представления.
        @Override
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            view.notifyConnectionStatusChanged(clientConnected);
        }
    }

}