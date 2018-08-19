import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebServer {

    static final int MAX_CLIENTS = 256;

    static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
    static final Locale DATE_LOCALE = Locale.US;
    static final String TIME_ZONE   = "GMT";

    static final Level LOG_LEVEL = Level.INFO;

    private static WebClientHandler[] clients = new WebClientHandler[MAX_CLIENTS];
    private static int nClients = 0;

    protected static SimpleDateFormat dateFormat;
    static {
        dateFormat = new SimpleDateFormat(DATE_FORMAT, DATE_LOCALE);
        dateFormat.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
    }

    /**
     * Ponto de entrada
     * 
     * @param args Argumentos da linha de comando
     */
    public static void main(String[] args) {
        Logger log = Logger.getGlobal();
        log.addHandler(new ConsoleHandler());
        log.setLevel(LOG_LEVEL);

        try (ServerSocket server = new ServerSocket(80))
        {
            while (true) {
                try {
                    Socket client = server.accept();

                    if (nClients < MAX_CLIENTS)
                        try {
                            (clients[nClients++] = new WebClientHandler(client) {
                                @Override
                                public void onClosed() {
                                    nClients--;
                                }
                            }).start();
                        } catch (IOException e) {
                            log.log(Level.SEVERE, "Failed to connect to client", e);
                        }
                    else {
                        log.log(Level.WARNING, "Server has reached its client limit");
                        client.close();
                    }
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Unexpected exception", e);
                }
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to connect to client", e);
        }
    }
}