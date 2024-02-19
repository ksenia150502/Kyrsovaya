package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.example.MARS.vectorGeneration;


public class SimpleGUI extends JFrame {
    private final JButton start1Button = new JButton("начать");
    private static final JTextField firstAbonent = new JTextField("", 5);
    private static final JTextField secondAbonent = new JTextField("", 5);
    private final JLabel input1Button = new JLabel("отправитель:");
    private final JLabel input2Button = new JLabel(" получатель: ");
    private final JLabel chooseAlgoritm = new JLabel("выберите алгоритм шифрования:             ");
    private final JRadioButton marsButton = new JRadioButton("mars");
    private final JRadioButton elgamalButton = new JRadioButton("elgamal");
    private final JButton chooseFileButton = new JButton("выбрать файл");
    private final JLabel fileNameLabel = new JLabel("Название выбранного файла: ");
    private final JLabel selectedFileNameLabel = new JLabel();
    private final JButton encryptButton = new JButton("шифрование данных");
    private final JButton decryptButton = new JButton("дешифрование данных");
    private File inputFile;
    private File encryptedFile;
    private final Elgamal elgamal = new Elgamal();


    public SimpleGUI() {

        super("шифрование данных");
        this.setBounds(100, 100, 450, 220);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Container container = this.getContentPane();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JPanel panel1 = new JPanel();
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
        panel1.add(input1Button);
        panel1.add(firstAbonent);
        container.add(panel1);

        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
        panel2.add(input2Button);
        panel2.add(secondAbonent);
        container.add(panel2);

        JPanel testPanel = new JPanel();
        testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.X_AXIS));
        ButtonGroup group = new ButtonGroup();
        testPanel.add(chooseAlgoritm);
        group.add(marsButton);
        group.add(elgamalButton);
        testPanel.add(marsButton);
        marsButton.setSelected(true);
        testPanel.add(elgamalButton);
        container.add(testPanel);


        JFileChooser fileChooser = new JFileChooser();

        ButtonChooseFile buttonChooseFile = new ButtonChooseFile(fileChooser);
        chooseFileButton.addActionListener(buttonChooseFile);


        JPanel chooseFile = new JPanel();
        chooseFile.setLayout(new BoxLayout(chooseFile, BoxLayout.X_AXIS));
        chooseFile.add(chooseFileButton);
        chooseFile.add(fileNameLabel);
        chooseFile.add(selectedFileNameLabel);
        container.add(chooseFile);
        container.add(start1Button);


        ButtonFolderProvider buttonFolderProvider = new ButtonFolderProvider();
        start1Button.addActionListener(buttonFolderProvider);





        KeyProvider keyProvider = new KeyGenerator();

        ButtonEncrypt buttonEncrypt = new ButtonEncrypt(keyProvider);
        encryptButton.addActionListener(buttonEncrypt);




        ButtonDecrypt buttonDecrypt = new ButtonDecrypt(keyProvider);
        decryptButton.addActionListener(buttonDecrypt);

        container.add(chooseFileButton);
        container.add(encryptButton);
        container.add(decryptButton);
    }

    public class ButtonChooseFile implements ActionListener {
        private final JFileChooser openedFile;
        private File selectedFile;
        public ButtonChooseFile(JFileChooser openedFile) {
            this.openedFile = openedFile;
        }
        public void actionPerformed(ActionEvent e) {
            List<String> folders = ButtonFolderProvider.getFolders();
            int ret = openedFile.showDialog(null, "Открыть файл");
            if (ret == JFileChooser.APPROVE_OPTION) {
                selectedFile = openedFile.getSelectedFile();
                inputFile = selectedFile;
                selectedFileNameLabel.setText(selectedFile.getName());
            }
            if (selectedFile != null) {
                System.out.println("Название выбранного файла при запуске программы: " + selectedFile.getName());
            }
        }
    }


    public interface KeyProvider {
        byte[] getKey();
    }

    public class KeyGenerator implements KeyProvider {
        private byte[] key;

        public byte[] getKey() {
            if (key == null)
                generateKey();
            return key;
        }

        public void generateKey() {
            key = vectorGeneration();
        }
    }

    class ButtonEncrypt implements ActionListener {
        private final KeyProvider keyProvider;
        private List<String> folders;

        public ButtonEncrypt(KeyProvider keyProvider) {
            this.keyProvider = keyProvider;
        }

        public void actionPerformed(ActionEvent e) {
            byte[] enc;
            folders = ButtonFolderProvider.getFolders();
            if (inputFile != null) {
                if (!folders.isEmpty() && folders.containsAll(List.of(firstAbonent.getText(), secondAbonent.getText()))) {
                    byte[] array;
                    try {
                        array = Files.readAllBytes(Paths.get(inputFile.getPath()));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    if (marsButton.isSelected()) {
                        enc = MARS.encrypt(array, keyProvider.getKey());
                    } else {
                        enc = elgamal.encrypt(array);
                    }

                    var splitted = inputFile.getName().split("\\.");
                    String name = splitted[0];
                    String extension = splitted[1];
                    String outFileName = folders.get(folders.size() - 1) + "/" + name + "_encrypted" + "." + extension;
                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outFileName))) {
                        out.write(enc);
                        encryptedFile = new File(outFileName);
                    } catch (FileNotFoundException exception) {
                        System.out.println("Ex");
                    } catch (IOException exception) {
                        System.out.println("Ex2");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Папки еще не созданы");
                }
            }
        }
    }

    class ButtonDecrypt implements ActionListener {
        private final KeyProvider keyProvider;

        public ButtonDecrypt(KeyProvider keyProvider) {
            this.keyProvider = keyProvider;
        }

        public void actionPerformed(ActionEvent e) {
            if (isExistsEncryptedFile()) {
                var splitted = inputFile.getName().split("\\.");
                String name = splitted[0];
                String extension = splitted[1];
                String outFileName = firstAbonent.getText() + "_" + secondAbonent.getText() + "/" + name + "_encrypted" + "." + extension;
                encryptedFile = new File(outFileName);
            }
            if (encryptedFile != null) {
                byte[] decrypt;
                List<String> folders = ButtonFolderProvider.getFolders();
                if (!folders.isEmpty() && folders.containsAll(List.of(firstAbonent.getText(), secondAbonent.getText()))) {
                    byte[] array;
                    try {
                        array = Files.readAllBytes(Paths.get(encryptedFile.getPath()));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    if (marsButton.isSelected()) {
                        decrypt = MARS.decrypt(array, keyProvider.getKey());
                    } else {
                        decrypt = elgamal.decrypt(array);
                    }
                    var t = elgamal.decrypt(array);
                    var splitted = inputFile.getName().split("\\.");
                    String name = splitted[0];
                    String extension = splitted[1];
                    String outFileName = folders.get(folders.size() - 2) + "/" + name + "_decrypted" + "." + extension;
                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outFileName))) {
                        out.write(decrypt);
                    } catch (FileNotFoundException exception) {
                        System.out.println("Ex");
                    } catch (IOException exception) {
                        System.out.println("Ex2");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Папки еще не созданы");
                }
            }
        }

        private boolean isExistsEncryptedFile() {
            var splitted = inputFile.getName().split("\\.");
            String name = splitted[0];
            String extension = splitted[1];
            String outFileName = firstAbonent.getText() + "_" + secondAbonent.getText() + "/" + name + "_encrypted" + "." + extension;
            return Files.exists(Paths.get(outFileName));
        }
    }

    class ButtonFolderProvider implements ActionListener {
        private static final List<String> folders = new ArrayList<>();

        public void actionPerformed(ActionEvent e) {
            createFolders();
        }

        private void createFolders() {
            createFolder(firstAbonent.getText());
            createFolder(secondAbonent.getText());
            createFolder(firstAbonent.getText() + "_" + secondAbonent.getText());
        }

        private void createFolder(String folderName) {
            File newFolder = new File(folderName);
            if (!newFolder.exists()) {
                newFolder.mkdir();
                JOptionPane.showMessageDialog(null, "Папка успешно создана!");
            } else {
                JOptionPane.showMessageDialog(null, "Папка уже существует!");
            }
            folders.add(folderName);
        }

        public static List<String> getFolders() {
            if (folders.isEmpty() && isInputFolderNameEmpty()) {
                folders.add(firstAbonent.getText());
                folders.add(secondAbonent.getText());
                folders.add(firstAbonent.getText() + "_" + secondAbonent.getText());
            }
            return folders;
        }

        private static boolean isInputFolderNameEmpty() {
            return firstAbonent.getText() != null && secondAbonent.getText() != null;
        }
    }
}
