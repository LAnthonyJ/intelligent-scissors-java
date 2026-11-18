import edu.princeton.cs.algs4.IndexMinPQ;
import java.awt.Point;
import java.util.*;

public class PathFinder {
    private static final float MAX_COST = 1e6f;
    private final ImageProcessor processor;
    private final float[][] costMap;
    private final Point[][] cameFrom;

    public PathFinder(ImageProcessor processor) {
        this.processor = processor;
        this.costMap = new float[processor.getWidth()][processor.getHeight()];
        this.cameFrom = new Point[processor.getWidth()][processor.getHeight()];
    }

    public void calculatePaths(Point seed) {
        // 初始化数据结构
        for (int y = 0; y < processor.getHeight(); y++) {
            Arrays.fill(costMap[y], MAX_COST);
            Arrays.fill(cameFrom[y], null);
        }
        costMap[seed.x][seed.y] = 0;

        // 使用优先队列优化
        IndexMinPQ<Float> pq = new IndexMinPQ<>(processor.getWidth() * processor.getHeight());
        pq.insert(toIndex(seed.x, seed.y), 0f);

        while (!pq.isEmpty()) {
            int minIndex = pq.delMin();
            Point current = toPoint(minIndex);

            // 遍历8邻域
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    processNeighbor(current, new Point(current.x + dx, current.y + dy), pq);
                }
            }
        }
    }

    private void processNeighbor(Point from, Point to, IndexMinPQ<Float> pq) {
        if (!isValid(to)) return;

        float newCost = costMap[from.x][from.y] + getMoveCost(from, to);
        if (newCost < costMap[to.x][to.y]) {
            costMap[to.x][to.y] = newCost;
            cameFrom[to.x][to.y] = from;

            int index = toIndex(to.x, to.y);
            if (pq.contains(index)) pq.decreaseKey(index, newCost);
            else pq.insert(index, newCost);
        }
    }

    private float getMoveCost(Point from, Point to) {
        float gradient = processor.getGradient(to.x, to.y);
        return 1.0f / (1.0f + gradient);  // 梯度越大，成本越低
    }

    public List<Point> getPathTo(Point target) {
        LinkedList<Point> path = new LinkedList<>();
        Point current = target;
        while (current != null) {
            path.addFirst(current);
            current = cameFrom[current.x][current.y];
        }
        return path;
    }

    private boolean isValid(Point p) {
        return p.x >= 0 && p.x < processor.getWidth()
                && p.y >= 0 && p.y < processor.getHeight();
    }

    private int toIndex(int x, int y) {
        return y * processor.getWidth() + x;
    }

    private Point toPoint(int index) {
        return new Point(index % processor.getWidth(), index / processor.getWidth());
    }

    // 简单占位实现判断路径是否闭环的方法，实际需更完善逻辑
    public boolean isPathClosed(List<Point> path) {
        if (path.size() < 3) {
            return false;
        }
        Point start = path.get(0);
        Point end = path.get(path.size() - 1);
        // 简单判断起点和终点距离较近视为闭环，实际可采用更精确算法
        return Math.sqrt(Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2)) < 10;
    }
}