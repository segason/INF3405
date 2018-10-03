
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    static Map<String,String> myMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.print("LANCEMENT DU SERVEUR\n\n");
        String ip = checkIP();
        int port = checkPort();
        readFromFile();

        ServerSocket serversocket;
        InetAddress locIP = InetAddress.getByName(ip);
        serversocket = new ServerSocket();
        serversocket.setReuseAddress(true);
        serversocket.bind(new InetSocketAddress(locIP, port));
        System.out.format("Application de filtres d'image lancée sur  %s:%d%n\n", ip, port);
        System.out.println("En attente d'utilisateurs...\n\n");

        try {
            while (true) {
                Socket socket = serversocket.accept();
                Thread thread = new Thread(new SobelFilter(socket));
                thread.start();

            }
        } finally {
            serversocket.close();
        }


    }



    //Fonction pour mettre à jour la map utiliser pour vérifier les identifiants et les mots de passe
    private static void readFromFile() {
        // TODO Auto-generated method stub
        File file = new File(System.getProperty("user.dir") + File.separator + "users.txt");
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(file));


            String line;
            String[] credentials;
            String user;
            String userPassWord;
            while ((line = br.readLine()) != null) {
                credentials = line.split(" ");
                user = credentials[0];
                userPassWord = credentials[1];
                myMap.put(user, userPassWord);

            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



    //Methode pour ecrire dans la base de données
    private static void writeDB(String userName, String password) {
        // TODO Auto-generated method stub

        File fichier = new File(System.getProperty("user.dir") + File.separator + "users.txt");
        FileWriter writer = null;
        BufferedWriter buffer = null;
        PrintWriter out = null;
        try {

            writer = new FileWriter(fichier, true);
            buffer = new BufferedWriter(writer);
            out = new PrintWriter(buffer);
            out.println(userName + " " + password);
            out.close();

        } catch (Exception e) {
            System.out.println("Impossible de creer le fichier");
        }
        finally{

            try {
                if(out != null)
                    out.close();
                if(buffer != null)
                    buffer.close();
                if(writer != null)
                    writer.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    //Methode pour verifier le port
    private static int checkPort() {
        //verification du port
        int port;
        boolean portBool = false;
        int lowerBound = 5000;
        int upperBound = 5050;

        do{
            System.out.println("Entrez le port:");
            Scanner reader = new Scanner(System.in);
            port = reader.nextInt();
            if(port >= lowerBound && port <= upperBound)
            {
                System.out.println("le port saisi est correct");
                portBool = true;
            }else{
                System.out.println("le port saisi n'est pas correct, veuillez ressayer");
            }

        }while(!portBool);

        return port;
    }

    //Methode pour verifier l'adresse ip
    private static String checkIP() {
        // TODO Auto-generated method stub
        boolean isIp;
        String serverAddress;

        do {
            System.out.println("Entrez l'adresse IP du serveur:");
            @SuppressWarnings("resource")
            Scanner reader = new Scanner(System.in);
            serverAddress = reader.next();

            isIp = IPAddressValidator(serverAddress);

            if (!isIp) {
                System.out.println("Veuillez verifier l adresse ip entrée");
            } else {
                System.out.println("Adresse Ip Correcte");
            }

        } while (!isIp);

        return serverAddress;
    }


//Methode appelee par la methode checkip pour verifier une adresse ip
//selon un pattern pre etablie

    public static boolean IPAddressValidator(String ip){

        Pattern pattern;
        Matcher matcher;

        pattern = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        matcher = pattern.matcher(ip);

        return matcher.find();
    }


    //Thread pour lancer l'application de traitement
    private static class SobelFilter implements Runnable {
        private BufferedReader inputBuffer;
        private PrintWriter output;
        private Socket socket;
        private String username;

        public SobelFilter(Socket socket) {

            this.socket = socket;
            try {
                inputBuffer = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                output = new PrintWriter(this.socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            checkUser();
            System.out.println("Le client {" + username + "} vient de se connecter!");
        }

        //Vérification de l'utilisateur
        private void checkUser(){
            String userName = null;
            String password = null;
            boolean userChecked;
            do{
                try {
                    userName = inputBuffer.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    password = inputBuffer.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(myMap.containsKey(userName)) {
                    if(myMap.containsValue(password)) {
                        userChecked = true;

                    }
                    else {
                        userChecked = false;
                    }
                }
                else{
                    myMap.put(userName, password);
                    writeDB(userName, password);
                    userChecked = true;

                }
                output.println(userChecked);

            }while(!userChecked);
            username = userName;
        }

        //Recevoir l'image sous forme de bytes envoyée par le client
        private byte[] receiveFile() throws IOException {
            int size  = Integer.parseInt(inputBuffer.readLine());
            byte[] bytes = new byte[size];
            for (int i = 0; i < size; i++) {
                bytes[i] = Byte.parseByte(inputBuffer.readLine());
            }
            return bytes;
        }

        //Envoyer l'image modifiée au client
        private void sendImage(BufferedImage newImage){
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(newImage, "jpg", baos);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] bytesSent = baos.toByteArray();

            int size = bytesSent.length;
            output.println(size);
            for (int i = 0; i < bytesSent.length; i++) {
                output.println(bytesSent[i]);
            }
        }

        //Afficher les étapes et informations sur le traitement de l'image
        private void displayReakTImeTreatmeantInfo(String fileName){
            DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            System.out.println("[" + username + " - " + socket.getLocalAddress().getHostAddress() +
                    ":" + socket.getLocalPort() + " - " + sdf.format(date) + "] : Image " +
                    fileName + ".jpg " + "reçue pour traitement.");
        }

        //Lancer le traitement de l'image
        public void run() {
            while (true) {
                try {
                    String fileName = inputBuffer.readLine();
                    byte[] bytes = receiveFile();
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                    displayReakTImeTreatmeantInfo(fileName);
                    BufferedImage newImage = Sobel.process(image);
                    sendImage(newImage);

                } catch (Exception e) {
                    try {
                        socket.close();
                        System.out.println("Session avec le client " + username + " terminée");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                break;
                }
            }
        }

    }
}
