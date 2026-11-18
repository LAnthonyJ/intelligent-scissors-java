import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;

public class ImageProcessor {
    private final float[][] gradientMap;
    private final int width, height;

    public ImageProcessor(BufferedImage image) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.gradientMap = new float[width][height];
        calculateGradients(image);
    }

    private void calculateGradients(BufferedImage image) {
        // Sobel算子核
        final int[][] sobelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        final int[][] sobelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float gx = 0, gy = 0;

                // 3x3邻域计算
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        Color color = new Color(image.getRGB(x + dx, y + dy));
                        float gray = (color.getRed() * 0.299f
                                + color.getGreen() * 0.587f
                                + color.getBlue() * 0.114f);
                        gx += gray * sobelX[dy + 1][dx + 1];
                        gy += gray * sobelY[dy + 1][dx + 1];
                    }
                }
                gradientMap[x][y] = (float) Math.sqrt(gx * gx + gy * gy);
            }
        }
        /**/
    }

    public float getGradient(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height)
            return 0;
        return gradientMap[x][y];
    }


//    public BufferedImage generateGrayImage(BufferedImage original) {
//        BufferedImage grayImage = new BufferedImage(
//                width, height, BufferedImage.TYPE_BYTE_GRAY
//        );
//
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                Color color = new Color(original.getRGB(x, y));
//                // 使用加权平均公式计算灰度值（推荐公式）[2,3,4](@ref)
//                float grayValue = color.getRed() * 0.299f
//                        + color.getGreen() * 0.587f
//                        + color.getBlue() * 0.114f;
//                int gray = (int) grayValue;
//                grayImage.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
//            }
//        }
//
//        // 保存灰度图[3,4](@ref)
//        ImageIO.write(grayImage, "PNG", new File("gray_output.png"));
//        return grayImage;
//    }



    public int getWidth() { return width; }
    public int getHeight() { return height; }
}