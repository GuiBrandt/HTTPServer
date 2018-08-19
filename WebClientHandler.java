import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gerenciador de cliente
 */
public abstract class WebClientHandler extends Thread {

    private static final String ERROR_BODY =    "<html>" + 
                                                "   <body>" + 
                                                "       <h1>Oooops...</h1>" +
                                                "       <h2>%d %s</h2>" + 
                                                "   </body>" + 
                                                "</html>";

    private Socket client;
    private BufferedReader input;
    private DataOutputStream output;

    private StringBuilder buffer;

    private HashMap<String, String> headers;

    private static Logger log;
    static {
        log = Logger.getGlobal();
    }

    /**
     * Construtor
     * 
     * @param client Cliente a ser tratado
     */
    public WebClientHandler(Socket client) throws IOException {
        this.client = client;

        input = new BufferedReader(
            new InputStreamReader(client.getInputStream()));
        output = new DataOutputStream(client.getOutputStream());

        buffer = new StringBuilder();

        headers = new HashMap<>();
        headers.put("Server", "Whatever/0.1.1 (???)");
    }

    /**
     * Processa uma requisição
     * 
     * @param head Primeira linha da requisição
     */
    private void processRequest(String head) throws IOException {
        String[] request = head.split(" ");
        for (
            String line = head; 
            input.ready() && line != null;
            line = input.readLine()
        )
            log.log(Level.FINE, line);

        if (request[0].equals("GET"))
            handleGETRequest(request[1]);
        else
            sendError(405, "Method Not Allowed");
    }

    /**
     * Escreve algo para o buffer
     * 
     * @param str String a ser escrita para o buffer
     */
    private void write(String str, Object... params) {
        str = new MessageFormat(str).format(params);

        buffer.append(str);
        log.log(Level.FINER, str);
    }

    /**
     * Escreve uma quebra de linha no buffer
     * 
     * @param str String a ser escrita para o buffer
     */
    private void writeln() {
        write("\r\n");
    }
    
    /**
     * Escreve algo para o buffer
     * 
     * @param str String a ser escrita para o buffer
     */
    private void writeln(String str, Object... params) {
        write(str, params);
        writeln();
    }

    /**
     * Apaga o buffer
     */
    private void clear() {
        buffer = new StringBuilder();
    }
    
    /**
     * Escreve do buffer para o cliente
     */
    private void flush() throws IOException {
        output.writeBytes(buffer.toString());
    }

    /**
     * Envia uma resposta ao cliente
     * 
     * @param code Código da resposta
     * @param message Mensagem da resposta
     */
    private void sendResponse(
        int code,
        String message
    ) throws IOException {
        clear();
        writeln("HTTP/1.1 {0} {1}", code, message);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            writeln("{0}: {1}", key, value);
        }
        
        writeln();
        flush();
    }

    /**
     * Envia uma resposta ao cliente
     * 
     * @param code Código da resposta
     * @param message Mensagem da resposta
     * @param contentType Tipo de conteúdo da resposta
     * @param content Conteúdo da resposta
     */
    private void sendResponse(
        int code,
        String message,
        String contentType,
        byte[] content
    ) throws IOException {
        headers.put("Content-Length",   "" + content.length);
        headers.put("Content-Type",     contentType);
        headers.put("Connection",       "Closed");
        sendResponse(code, message);

        output.write(content);
    }

    /**
     * Envia uma mensagem de erro HTTP para o cliente
     * 
     * @param code Código do erro
     * @param message Mensagem de erro
     */
    private void sendError(int code, String message) throws IOException  {
        String content = String.format(ERROR_BODY, code, message);
        sendResponse(code, message, "text/html", content.getBytes());
    }

    /**
     * Determina o tipo de conteúdo para um arquivo
     * 
     * @param filename Nome do arquivo
     */
    private String fileContentType(String filename) {
        return URLConnection.guessContentTypeFromName(filename);
    }

    /**
     * Lida com uma requisição GET do cliente
     * 
     * @param fileName Nome do arquivo requisitado
     */
    private void handleGETRequest(String fileName) throws IOException {
        if (fileName.isEmpty() || fileName.endsWith("/"))
        {
            headers.put("Location", "/index.html");
            sendResponse(302, "Found");
        }
        else
        {
            headers.put("Date", WebServer.dateFormat.format(new Date()));

            try {
                File file = new File(new File(".").getCanonicalPath() + "/htdocs/" + fileName);

                String contentType = fileContentType(fileName);
                byte[] content = Files.readAllBytes(file.toPath());

                headers.put("Last-Modified", WebServer.dateFormat.format(file.lastModified()));
                sendResponse(200, "OK", contentType, content);
            } catch (NoSuchFileException e) {
                log.log(Level.WARNING, "File not found: {0}", fileName);
                sendError(404, "Not Found");
            }
        }
    }

    /**
     * Execução da thread
     */
    @Override
    public void run() {
        try {
            String line = input.readLine();

            if (line != null) {
                log.log(Level.INFO, line);
                processRequest(line);

                output.flush();
            }
            
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to read from client", e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {}

            onClosed();
        }
    }

    /**
     * Evento disparado quando a conexão com o cliente é fechada
     */
    public abstract void onClosed();
}