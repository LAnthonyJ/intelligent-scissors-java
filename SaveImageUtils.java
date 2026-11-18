import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SaveImageUtils {
    public static void saveCroppedImage(BufferedImage croppedImage, JFrame parentFrame) {
        if (croppedImage == null) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存抠图");
        int userSelection = fileChooser.showSaveDialog(parentFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                ImageIO.write(croppedImage, "png", fileToSave);
                JOptionPane.showMessageDialog(parentFrame, "保存成功");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parentFrame, "保存失败：" + ex.getMessage());
            }
        }
    }
}