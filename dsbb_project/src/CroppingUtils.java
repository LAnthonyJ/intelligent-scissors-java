import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.List;

public class CroppingUtils {
    public static BufferedImage[] cropImage(BufferedImage originalImage, List<java.awt.Point> path) {
        if (path.isEmpty()) {
            return null;
        }

        // 创建多边形
        Polygon polygon = new Polygon();
        for (java.awt.Point p : path) {
            polygon.addPoint(p.x, p.y);
        }

        // 创建区域并与图像边界相交
        Area area = new Area(polygon);
        Rectangle imageBounds = new Rectangle(0, 0, originalImage.getWidth(), originalImage.getHeight());
        area.intersect(new Area(imageBounds));

        // 在原图上绘制半透明红色的截取区域
        BufferedImage resultImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());
        Graphics2D g2dResult = resultImage.createGraphics();
        g2dResult.drawImage(originalImage, 0, 0, null);
        // 设置 30% 透明度的红色
        Color semiTransparentRed = new Color(255, 0, 0, (int) (255 * 0.3));
        g2dResult.setColor(semiTransparentRed);
        g2dResult.fill(area);
        g2dResult.dispose();

        // 创建一个与原始图像大小相同的透明背景的 BufferedImage
        BufferedImage croppedImageFullSize = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dCroppedFullSize = croppedImageFullSize.createGraphics();
        g2dCroppedFullSize.setClip(area);
        g2dCroppedFullSize.drawImage(originalImage, 0, 0, null);
        g2dCroppedFullSize.dispose();

        return new BufferedImage[]{resultImage, croppedImageFullSize};
    }
}