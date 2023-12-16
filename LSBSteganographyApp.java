import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import javax.imageio.ImageIO;

public class LSBSteganographyApp {

    private JFrame frame;
    private JButton encodeButton;
    private JTextField filePathField, outputDirectoryField, outputFileNameField, messageField;
    private JLabel originalImageLabel, encodedImageLabel, lsbImageLabel;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                LSBSteganographyApp window = new LSBSteganographyApp();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public LSBSteganographyApp() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Steganography App");
        frame.setBounds(100, 100, 1200, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());


        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(5, 2, 5, 10));  // Increased vertical gap

        inputPanel.add(new JLabel("Путь к изображению:"));
        filePathField = new JTextField();
        inputPanel.add(filePathField);

        inputPanel.add(new JLabel("Путь для сохранения изображения:"));
        outputDirectoryField = new JTextField();
        inputPanel.add(outputDirectoryField);

        inputPanel.add(new JLabel("Нзвание изображения:"));
        outputFileNameField = new JTextField();
        inputPanel.add(outputFileNameField);

        inputPanel.add(new JLabel("Сообщение для шифрования:"));
        messageField = new JTextField();
        inputPanel.add(messageField);

        encodeButton = new JButton("Запустить программу");
        encodeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                encodeButtonClicked();
            }
        });
        inputPanel.add(encodeButton);

        frame.getContentPane().add(inputPanel, BorderLayout.WEST);


        JPanel imagePanel = new JPanel();
        imagePanel.setLayout(new GridLayout(1, 3, 10, 10));

        originalImageLabel = new JLabel("Исходное изображение");
        originalImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePanel.add(originalImageLabel);

        encodedImageLabel = new JLabel("Полученное изображение");
        encodedImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePanel.add(encodedImageLabel);

        lsbImageLabel = new JLabel("Младшие биты");
        lsbImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePanel.add(lsbImageLabel);

        frame.getContentPane().add(imagePanel, BorderLayout.CENTER);
    }

    private void encodeButtonClicked() {
        String filePath = filePathField.getText();
        String outputDirectory = outputDirectoryField.getText();
        String outputFileName = outputFileNameField.getText();
        String message = messageField.getText();

        try {
            File inFile = new File(filePath);
            BufferedImage initImage = ImageIO.read(inFile);


            ImageIcon originalIcon = new ImageIcon(initImage.getScaledInstance(300, 300, Image.SCALE_DEFAULT));
            originalImageLabel.setIcon(originalIcon);

            String bitMsg = Encode.encodeMessage(message);
            BufferedImage newImage = Encode.encodeImage(bitMsg, initImage);


            ImageIcon encodedIcon = new ImageIcon(newImage.getScaledInstance(300, 300, Image.SCALE_DEFAULT));
            encodedImageLabel.setIcon(encodedIcon);


            BufferedImage lsbImage = getLsbImage(newImage);
            ImageIcon lsbIcon = new ImageIcon(lsbImage.getScaledInstance(300, 300, Image.SCALE_DEFAULT));
            lsbImageLabel.setIcon(lsbIcon);

            File dir = new File(outputDirectory);
            if (dir.exists() && dir.isDirectory() && dir.canWrite()) {
                File finalImage = new File(dir, outputFileName + ".png");
                ImageIO.write(newImage, "png", finalImage);
                JOptionPane.showMessageDialog(frame, "Изображение успешно сохранено!");
            } else {
                throw new IOException("Неправильно указана директория");
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private BufferedImage getLsbImage(BufferedImage image) {
        BufferedImage lsbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Color originalColor = new Color(image.getRGB(x, y));
                int lsb = originalColor.getRed() & 1;


                Color lsbColor = new Color(lsb * 255, lsb * 255, lsb * 255);
                lsbImage.setRGB(x, y, lsbColor.getRGB());
            }
        }

        return lsbImage;
    }
}

final class Encode {

    static String encodeMessage(String message) {
        String bitString = new BigInteger(message.getBytes()).toString(2);

        if (bitString.length() % 8 != 0) {
            String zeroes = "";
            while ((bitString.length() + zeroes.length()) % 8 != 0) {
                zeroes = zeroes + "0";
            }
            bitString = zeroes + bitString;
        }

        return bitString;
    }

    static BufferedImage encodeImage(String bit, BufferedImage image) {
        int pointer = bit.length() - 1;

        for (int x = image.getWidth() - 1; x >= 0; x--) {
            for (int y = image.getHeight() - 1; y >= 0; y--) {

                Color c = new Color(image.getRGB(x, y));
                byte r = (byte) c.getRed();
                byte g = (byte) c.getGreen();
                byte b = (byte) c.getBlue();
                byte[] RGB = {r, g, b};
                byte[] newRGB = new byte[3];

                for (int i = 2; i >= 0; i--) {
                    if (pointer >= 0) {
                        int lsb;
                        if ((RGB[i] & 1) == 1) {
                            lsb = 1;
                        } else {
                            lsb = 0;
                        }


                        if (Character.getNumericValue(bit.charAt(pointer)) != lsb) {
                            if (lsb == 1) {
                                newRGB[i] = (byte) (RGB[i] & ~(1));
                            } else {
                                newRGB[i] = (byte) (RGB[i] | 1);
                            }
                        } else {
                            newRGB[i] = RGB[i];
                        }
                    } else {

                        newRGB[i] = (byte) (RGB[i] & ~(1));
                    }

                    pointer--;
                }

                Color newColor = new Color(Byte.toUnsignedInt(newRGB[0]), Byte.toUnsignedInt(newRGB[1]), Byte.toUnsignedInt(newRGB[2]));
                image.setRGB(x, y, newColor.getRGB());
            }
        }
        return image;
    }
}
