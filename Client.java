import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Client {

    private static Socket sock;
    private static String fileName;
    private static BufferedReader stdin;
    private static PrintStream os;

    public static void main(String[] args) throws IOException {
        String serverIP = String.valueOf(InetAddress.getLocalHost().getHostAddress());
        while(true) {
            try {
                sock = new Socket(serverIP, 25444);
                stdin = new BufferedReader(new InputStreamReader(System.in));
            } catch (Exception e) {
                System.err.println("Cannot connect to the server, try again later.");
                System.exit(1);
            }

            os = new PrintStream(sock.getOutputStream());

            try {
                switch (Integer.parseInt(selectAction())) {
                    case 1:
                        os.println("1");
                        sendFile();
                        continue;
                    case 2:
                        os.println("2");
                        System.out.print("Enter file name: ");
                        fileName = stdin.readLine();
                        os.println(fileName);
                        receiveFile(fileName);
                        continue;
                    case 3:
                        sock.close();
                        System.exit(1);
                }
            } catch (Exception e) {
                System.err.println("not valid input");
            }

        }

    }

    public static String selectAction() throws IOException {
        System.out.println("1. Send file.");
        System.out.println("2. Recieve file.");
        System.out.println("3. Exit.");
        System.out.print("\nMake selection: ");

        return stdin.readLine();
    }


    public static void sendFile() {
        try {
            System.out.print("Enter file name: ");
            fileName = stdin.readLine();

            File myFile = new File(fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];
            if(!myFile.exists()) {
                System.out.println("File does not exist..");
                return;
            }

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            //bis.read(mybytearray, 0, mybytearray.length);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);

            OutputStream os = sock.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();
            System.out.println("File "+fileName+" sent to Server.");
        } catch (Exception e) {
            System.err.println("Exception: "+e);
        }
    }

    public static void receiveFile(String fileName) {
        try {
            int bytesRead;
            InputStream in = sock.getInputStream();

            DataInputStream clientData = new DataInputStream(in);

            fileName = clientData.readUTF();
            OutputStream output = new FileOutputStream(fileName);
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            Path path = Paths.get(fileName);
            byte[] bytes = Files.readAllBytes(path);
            String text = new String(bytes);
            text = text.toLowerCase();

            Pattern r = Pattern.compile("\\p{Alpha}+");
            Matcher matcher = r.matcher(text);
            Map<String, Integer> freq = new HashMap<>();
            while (matcher.find()) {
                String word = matcher.group();
                Integer current = freq.getOrDefault(word, 0);
                freq.put(word, current + 1);
            }

            List<Map.Entry<String, Integer>> entries = freq.entrySet()
                    .stream()
                    .sorted((i1, i2) -> Integer.compare(i2.getValue(), i1.getValue()))
                    .limit(10)
                    .collect(Collectors.toList());

            System.out.println("Rank  Word  Frequency");
            System.out.println("====  ====  =========");
            int rank = 1;
            for (Map.Entry<String, Integer> entry : entries) {
                String word = entry.getKey();
                Integer count = entry.getValue();
                System.out.printf("%2d    %-4s    %5d\n", rank++, word, count);
            }



            output.close();
            in.close();

            System.out.println("File "+fileName+" received from Server.");
        } catch (IOException ex) {
            System.out.println("Exception: "+ex);
        }

    }
}