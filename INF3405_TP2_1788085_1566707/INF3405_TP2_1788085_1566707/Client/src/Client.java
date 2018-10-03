import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
* Classe représentant l'implémentation du client dans l'application
* */
public class Client implements Runnable{

    private BufferedReader in;
    private Socket socket;
    private PrintWriter output;
    private Scanner reader = new Scanner(System.in);
    String JPG = ".jpg";


    //Fonction servant à lancer le client
    @SuppressWarnings("resource")
    public void run() {

        System.out.print("LANCEMENT DU CLIENT\n\n");

        //Demander au client de donner une adresse IP valide
        String serverAddress = checkIP();

        //verification du port
        int port = checkPort();

        try {
            socket = new Socket(serverAddress, port);
        } catch (IOException e) {
            System.out.println("Le serveur n'a pas été lancé!");
        }

        if(socket != null) {
            System.out.format("Application de filtres d'image lancée sur %s:%d%n \n", serverAddress, port);
            checkCredentials();
            sobelTreatment();
            try {
                closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Fonction servant à valider l'identifiant et le mot de passe du client
    private void checkCredentials(){
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean userIsChecked = false;
        do {
            System.out.println("Veuillez saisir le nom d utilisateur :");
            String username = reader.next();
            output.println(username);

            System.out.println("Veuillez saisir le mot de passe :");
            String password = reader.next();
            output.println(password);
            try {
                userIsChecked = Boolean.parseBoolean(in.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }


            if(userIsChecked){
                System.out.println(username + ", vous venez de vous connecter!");
            }
            else
                System.out.println("Erreur dans le mot de passe! Veuillez réentrer vos informations\n");
        }while(!userIsChecked);
    }

    //Fonction servant à valider l'adresse IP du client
    private String checkIP(){
        boolean isIP;
        String serverAddress;
        do{

            System.out.println("Entrez l'adresse IP du serveur:");

            serverAddress = reader.next();

            isIP = IPAddressValidator(serverAddress);

            if(!isIP)
            {
                System.out.println("Veuillez verifier l adresse ip entrée");
            }else{
                System.out.println("Adresse Ip Correcte");
            }

        }while(!isIP);

        return serverAddress;
    }


    //Fonction servant à valider le port de connexion
    private int checkPort(){
        int port;
        int lowerBound = 5000;
        int upperBound = 5050;
        boolean portBool = false;
        do{
            System.out.println("Entrez le port:");
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

    //pour verifier une adresse ip selon un pattern
    private boolean IPAddressValidator(String ip){

        Pattern pattern;
        Matcher matcher;

        pattern = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        matcher = pattern.matcher(ip);

        return matcher.find();
    }

    //Déclenchement du traitement d'images
    public void sobelTreatment(){

        System.out.println("Bienvenue au traitement d'images avec le filtre Sobel!\n" +
                "Vous pouvez entrer le nom de l'image sur laquelle vous voulez appliquer le filtre!\n" +
                "Donnez également le nom de l'image après le traitment\n" +
                "Mettre seulement le nom du fichier SANS L'EXTENSION. Ex: image.jpg => image ." +
                "\nEntrer '0' comme nom d'image lorsque vous voulez quitter.\n\n");

        boolean isFinished = false;
        boolean exist;
        String fileName;
        String newFileName;
        String path;

        do{
            printFileNamesInDirectory();
            System.out.print("\nVeuillez entrer le nom de l'image à modifier: ");
            fileName = reader.next();
            if(fileName.equals("0")){
                isFinished = true;
            }
            else {
                path = System.getProperty("user.dir") + File.separator + fileName + JPG;
                exist = new File(path).exists();
                if (!exist) {
                    System.out.println("L'image n'existe pas. Entrer le nom d'une image valide!\n");
                } else {
                    System.out.print("Veuillez entrer le nom de la nouvelle image modifiée (même format que l'ancienne image): ");
                    newFileName = reader.next();
                    try {
                        output.println(fileName);
                        sendImage(fileName);
                        File receivedFile = receiveFile(newFileName);
                        BufferedImage image = ImageIO.read(receivedFile);
                        System.out.println("Le traitement de votre image est terminé!");
                        System.out.println("Votre image a été enregistrée à cet endroit " + receivedFile.getAbsolutePath());
                        System.out.println("\nVous pouvez continuer à envoyer des images ou entrer '0' comme nom d'image pour quitter!\n\n");
                        displayImage(image);

                    } catch (Exception e) {
                        System.out.println("Une erreur a causé la fin de votre communication avec le serveur.");
                        break;
                    }
                }
            }

        }while(!isFinished);
    }


    //Fonction pour sauvegarder la nouvelle image reçue
    private void saveFile(File file){
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    //Fonction pour recevoir et sauvegarder la nouvelle image traitée par le serveur
    private File receiveFile(String fileName) throws IOException {
        String path = fileName + JPG;
        File file = new File(path);
        saveFile(file);
        int size  = Integer.parseInt(in.readLine());
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = Byte.parseByte(in.readLine());
        }

        FileOutputStream fos = new FileOutputStream(path);
        fos.write(bytes);
        return file;
    }


    //Fonction pour afficher les images JPEG présentes dans le répertoire actuel
    private void printFileNamesInDirectory(){
        System.out.println("\nVoici les fichiers présents dans le dossier courant:");
        File folder = new File(System.getProperty("user.dir"));
        File[] listofFIles = folder.listFiles();
        if(listofFIles != null) {
            for (int i = 0; i < listofFIles.length; i++) {
                File file = listofFIles[i];
                if (file.isFile() && file.getName().contains(JPG)) {
                    System.out.println(listofFIles[i].getName());
                }
            }
        }
    }

    //Fonction d'affichage d'image
    private void displayImage(BufferedImage image) {

        System.out.println("Lecture de l'image en cours...");
        JFrame frame = new JFrame();
        frame.setSize(image.getWidth(), image.getHeight());
        JLabel label = new JLabel(new ImageIcon(image));
        frame.add(label);
        frame.setVisible(true);
        System.out.println("Affichage...");
    }

    //Fonction d'envoi d'image
    private  void sendImage(String fileName) throws IOException{
        // TODO Auto-generated method stub
        String path = System.getProperty("user.dir") + File.separator + fileName + JPG;
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        int size = bytes.length;
        output.println(size);
        for (int i = 0; i < bytes.length; i++) {
            output.println(bytes[i]);
        }

        System.out.println("Votre image " + fileName + JPG + " a été envoyée au serveur.\n");

    }
    private  void closeConnection() throws IOException {
        // TODO Auto-generated method stub
        socket.close();
        System.out.println("Vous avez terminé votre session");

    }

    public static void main(String[] args) {
        Thread thread = new Thread(new Client());
        thread.start();
        thread.interrupt();
    }



}
