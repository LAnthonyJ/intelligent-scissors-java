import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.imageio.ImageIO;

public class IntelligentScissorsGUI extends JFrame {
    private BufferedImage originalImage;
    private ImageProcessor imageProcessor;
    private PathFinder pathFinder;
    private Point seedPoint;
    private Point currentPoint;
    private Point pre_seed;
    private Point origin_point = null;
    private boolean isDrawingFinished = false;

    List<java.awt.Point> path = new ArrayList<>();

    private final Deque<Point> pathHistory = new ArrayDeque<>(100);
    private static final int MIN_STABLE_POINTS = 10;
    private static final double STABILITY_VARIANCE_THRESHOLD = 2.5;
    private static final int index = 30;
    private int stableCount = 0;
    private boolean isCoolingDown = false;
    private Point lastStablePoint = null;

    private JLabel statusLabel;
    private JLabel imageLabel;

    public IntelligentScissorsGUI() {
        initGUI();
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initGUI() {
        // 创建菜单栏
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("文件");
        JMenuItem openItem = new JMenuItem("打开图像");
        JMenuItem saveItem = new JMenuItem("保存原始图像");
        JMenuItem saveCroppedItem = new JMenuItem("保存抠图");
        openItem.addActionListener(this::openImage);
        saveItem.addActionListener(this::saveOriginalImageActionPerformed);
        saveCroppedItem.addActionListener(e -> showCroppedImage());
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveCroppedItem);
        menuBar.add(fileMenu);

        // 创建状态栏
        statusLabel = new JLabel("就绪");
        add(statusLabel, BorderLayout.SOUTH);

        // 创建图像显示区域
        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (originalImage != null) {
                    g.drawImage(originalImage, 0, 0, null);
                    drawLiveWire(g);
                }
            }
        };
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (!isDrawingFinished) {
                        if (!pathFinder.isPathClosed(path)) {
                            if (path.size() > 0 && origin_point != null) {
                                List<Point> closingPath = pathFinder.getPathTo(origin_point);
                                // 添加同步块
                                synchronized (path) {
                                    for (Point p : closingPath) {
                                        path.add(new java.awt.Point(p.x, p.y));
                                    }
                                }
                            }
                        }
                        isDrawingFinished = true;
                        showCroppedImage();
                    }
                } else {
                    if (!isDrawingFinished) {
                        pre_seed = seedPoint;
                        seedPoint = snapToEdge(new Point(e.getX() - imageLabel.getX(), e.getY() - imageLabel.getY()));
                        if (pre_seed == null) {
                            pre_seed = seedPoint;
                            origin_point = seedPoint;
                        }
                        new Thread(() -> pathFinder.calculatePaths(pre_seed)).start();
                        List<Point> last_path = pathFinder.getPathTo(seedPoint);
                        // 添加同步块
                        synchronized (path) {
                            for (Point p : last_path) {
                                path.add(new java.awt.Point(p.x, p.y));
                            }
                        }
                        new Thread(() -> pathFinder.calculatePaths(seedPoint)).start();
                        updateStatus("种子点已设置: " + seedPoint);
                        pathHistory.add(seedPoint);
                    }
                }
            }
        });
        imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!isDrawingFinished) {
                    // 确保获取的是图像内的正确坐标
                    currentPoint = snapToEdge(new Point(e.getX() - imageLabel.getX(), e.getY() - imageLabel.getY()));
                    imageLabel.repaint();
                    pathHistory.add(currentPoint);

                    if (isCoolingDown) {
                        if (stableCount < index) {
                            stableCount++;
                            currentPoint = lastStablePoint;
                        } else {
                            isCoolingDown = false;
                            stableCount = 0;
                        }
                    } else {
                        if (checkPathStability()) {
                            isCoolingDown = true;
                            stableCount = 0;
                            lastStablePoint = currentPoint;
                        }
                    }
                }
            }
        });

        add(new JScrollPane(imageLabel), BorderLayout.CENTER);
        setJMenuBar(menuBar);
    }

    private void openImage(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                originalImage = ImageIO.read(fc.getSelectedFile());
                imageProcessor = new ImageProcessor(originalImage);
                pathFinder = new PathFinder(imageProcessor);
                imageLabel.setPreferredSize(new Dimension(
                        originalImage.getWidth(), originalImage.getHeight()));
                repaint();
                updateStatus("已加载图像: " + fc.getSelectedFile().getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "图像加载失败");
            }
        }
    }

    private void drawLiveWire(Graphics g) {
        if (seedPoint == null || currentPoint == null) return;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        List<Point> current_path = pathFinder.getPathTo(currentPoint);

        Point prev = null;
        for (Point p : path) {
            if (prev != null) {
                g2d.drawLine(prev.x, prev.y, p.x, p.y);
            }
            prev = p;
        }
        for (Point p : current_path) {
            if (prev != null) {
                g2d.drawLine(prev.x, prev.y, p.x, p.y);
            }
            prev = p;
        }
        g2d.dispose();
        if (checkPathStability()) {
            autoCreateNewSeed();
        }
    }

    private void updateStatus(String text) {
        statusLabel.setText(text);
    }

    private Point snapToEdge(Point rawPoint) {
        final int SNAP_RADIUS = 10;
        float maxGradient = -10;
        Point bestPoint = rawPoint;

        for (int dx = -SNAP_RADIUS; dx <= SNAP_RADIUS; dx++) {
            for (int dy = -SNAP_RADIUS; dy <= SNAP_RADIUS; dy++) {
                int x = rawPoint.x + dx;
                int y = rawPoint.y + dy;
                if (imageProcessor != null) {
                    if (x >= 0 && x < imageProcessor.getWidth() &&
                            y >= 0 && y < imageProcessor.getHeight()) {
                        float g = imageProcessor.getGradient(x, y);
                        if (g > maxGradient) {
                            maxGradient = g;
                            bestPoint = new Point(x, y);
                        }
                    }
                }
            }
        }
        return bestPoint;
    }

    private boolean checkPathStability() {
        if (pathHistory.size() < MIN_STABLE_POINTS) return false;

        double meanX = pathHistory.stream().mapToDouble(p -> p.x).average().orElse(0);
        double meanY = pathHistory.stream().mapToDouble(p -> p.y).average().orElse(0);

        double variance = pathHistory.stream()
                .mapToDouble(p -> Math.pow(p.x - meanX, 2) + Math.pow(p.y - meanY, 2))
                .average().orElse(0);

        return variance < STABILITY_VARIANCE_THRESHOLD;
    }

    private void autoCreateNewSeed() {
        if (currentPoint == null) return;

        seedPoint = currentPoint;
        // 添加同步块
        synchronized (path) {
            path.addAll(pathFinder.getPathTo(seedPoint));
        }
        pathHistory.clear();

        new Thread(() -> {
            pathFinder.calculatePaths(seedPoint);
            SwingUtilities.invokeLater(() -> {
                imageLabel.repaint();
                statusLabel.setText("新种子点创建：" + seedPoint);
            });
        }).start();
    }

    private void saveOriginalImageActionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "保存原始图像功能未实现");
    }

    private void showCroppedImage() {
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "尚未完成抠图路径绘制");
            return;
        }
        BufferedImage[] images = CroppingUtils.cropImage(originalImage, path);
        if (images != null) {
            // 更新显示的原图
            originalImage = images[0];
            imageLabel.repaint();

            // 显示抠下的图像弹窗
            JFrame croppedFrame = new JFrame("Cropped Image");
            croppedFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JLabel croppedLabel = new JLabel(new ImageIcon(images[1]));
            JButton saveButton = new JButton("保存");
            saveButton.addActionListener(evt -> {
                SaveImageUtils.saveCroppedImage(images[1], croppedFrame);
            });

            JPanel croppedPanel = new JPanel(new BorderLayout());
            croppedPanel.add(croppedLabel, BorderLayout.CENTER);
            croppedPanel.add(saveButton, BorderLayout.SOUTH);

            croppedFrame.add(croppedPanel);
            croppedFrame.pack();
            croppedFrame.setLocationRelativeTo(this);
            croppedFrame.setVisible(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new IntelligentScissorsGUI().setVisible(true);
        });
    }
}