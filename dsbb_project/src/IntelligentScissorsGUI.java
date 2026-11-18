
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
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

    private final Deque<Point> pathHistory = new ArrayDeque<>(10000);

    private JLabel statusLabel;
    private JLabel imageLabel;

    private JCheckBox snapCheckBox;
    private JCheckBox coolingCheckBox;



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
                }
            }
        });

        add(new JScrollPane(imageLabel), BorderLayout.CENTER);
        setJMenuBar(menuBar);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 光标吸附开关
        snapCheckBox = new JCheckBox("启用光标吸附", true);
        snapCheckBox.setToolTipText("自动对齐到图像边缘");
        controlPanel.add(snapCheckBox);

        // 路径冷却开关
        coolingCheckBox = new JCheckBox("启用路径冷却", true);
        coolingCheckBox.setToolTipText("路径稳定时自动优化");
        controlPanel.add(coolingCheckBox);

        add(controlPanel, BorderLayout.NORTH);
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
//        if (isPathCoolingEnabled)
            processPathUpdate(current_path);

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
        drawStabilityHints(g2d, current_path);
        g2d.dispose();

    }



    private void updateStatus(String text) {
        statusLabel.setText(text);
    }

    private Point snapToEdge(Point rawPoint) {
        final int SNAP_RADIUS = 5;
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

    //路径冷却
    // 在GUI类中添加成员变量
    private final Deque<List<PathNode>> pathObservationQueue = new ArrayDeque<List<PathNode>>();
    private static final int OBSERVATION_WINDOW = 15; // 观察窗口大小
    private static final int STABILITY_THRESHOLD = 30; // 稳定阈值

    private void processPathUpdate(List<Point> currentPath) {
        // 转换当前路径为观察节点
        List<PathNode> currentNodes = currentPath.stream()
                .map(p -> new PathNode(p))
                .collect(Collectors.toList());

        // 初始化队列
        if (pathObservationQueue.isEmpty()) {
            pathObservationQueue.addLast(currentNodes);
            return;
        }

        // 获取最近一次观察结果
        List<PathNode> lastObservation = pathObservationQueue.peekLast();

        // 遍历当前路径节点更新稳定计数
        for (int i = 0; i < currentNodes.size(); i++) {
            PathNode currentNode = currentNodes.get(i);

            // 只比较有效范围内的节点
            if (i < lastObservation.size()) {
                PathNode lastNode = lastObservation.get(i);
                if (currentNode.samePosition(lastNode.point)) {
                    currentNode.stableCount = lastNode.stableCount + 1;
                } else {
                    currentNode.stableCount = 0;
                }
            }
        }

        // 维护观察窗口
        pathObservationQueue.addLast(currentNodes);
        if (pathObservationQueue.size() > OBSERVATION_WINDOW) {
            pathObservationQueue.removeFirst();
        }

        // 检测稳定节点
        detectStableNodes(currentNodes);
    }

    private void detectStableNodes(List<PathNode> currentNodes) {
        // 从后往前查找第一个稳定节点
        for (int i = currentNodes.size()-1; i >= 0; i--) {
            PathNode node = currentNodes.get(i);
            if (node.stableCount >= STABILITY_THRESHOLD) {
                updateSeedPoint(node.point);
                for(int j=0;j<=i;j++){
                    PathNode Node = currentNodes.get(j);
                    path.add(Node.point);
                }
                break;
            }
        }
    }

    private void updateSeedPoint(Point newSeed) {
        SwingUtilities.invokeLater(() -> {
            seedPoint = newSeed;
            pathFinder.calculatePaths(seedPoint);
            imageLabel.repaint();
            statusLabel.setText("自动更新种子点至: ("
                    + newSeed.x + ", " + newSeed.y + ")");
        });
    }

    private void drawStabilityHints(Graphics2D g2d, List<Point> path) {
        List<PathNode> currentNodes = pathObservationQueue.peekLast();
        if (currentNodes == null) return;

        g2d.setColor(new Color(0, 255, 0, 100));
        for (int i = 0; i < currentNodes.size(); i++) {
            PathNode node = currentNodes.get(i);
            if (node.stableCount > 0) {
                Point p = path.get(i);
                int radius = Math.min(4, node.stableCount);
                g2d.fillOval(p.x-radius, p.y-radius, radius, radius);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new IntelligentScissorsGUI().setVisible(true);
        });
    }
}

